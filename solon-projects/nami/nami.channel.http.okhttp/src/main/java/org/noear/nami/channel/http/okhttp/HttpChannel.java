package org.noear.nami.channel.http.okhttp;

import okhttp3.MediaType;
import okhttp3.Response;
import org.noear.nami.*;
import org.noear.nami.common.Constants;
import org.noear.nami.common.ContentTypes;

/**
 * Http 通道
 * */
public class HttpChannel implements Channel {
    public static final HttpChannel instance = new HttpChannel();

    @Override
    public Result call(Context ctx) throws Throwable {
        pretreatment(ctx);

        //0.检测method
        boolean is_get = Constants.METHOD_GET.equals(ctx.action);
        String url = ctx.url;

        //0.尝试重构url
        if (is_get && ctx.args.size() > 0) {
            StringBuilder sb = new StringBuilder(ctx.url);
            //如果URL中含有固定参数,应该用'&'添加参数
            sb.append(ctx.url.contains("?") ? "&" : "?");

            ctx.args.forEach((k, v) -> {
                if (v != null) {
                    sb.append(k).append("=")
                            .append(HttpUtils.urlEncode(v.toString()))
                            .append("&");
                }
            });

            url = sb.substring(0, sb.length() - 1);
        }

        if (ctx.config.getDecoder() == null) {
            throw new IllegalArgumentException("There is no suitable decoder");
        }

        //0.尝试解码器的过滤
        ctx.config.getDecoder().pretreatment(ctx);

        //0.开始构建http
        HttpUtils http = HttpUtils.http(url).headers(ctx.headers).timeout(ctx.config.getTimeout());
        Response response = null;
        Encoder encoder = ctx.config.getEncoder();

        //1.执行并返回
        if (is_get || ctx.args.size() == 0) {
            response = http.exec(Constants.METHOD_GET);
        } else {
            if (encoder == null) {
                String ct0 = ctx.headers.getOrDefault(Constants.HEADER_CONTENT_TYPE, "");

                if (ct0.length() == 0) {
                    response = http.data(ctx.args).exec(ctx.action);
                } else {
                    encoder = NamiManager.getEncoder(ct0);
                }
            } else {
                encoder = ctx.config.getEncoder();
            }
        }

        if (response == null && encoder != null) {
            byte[] bytes = encoder.encode(ctx.body);

            if (bytes != null) {
                response = http.bodyRaw(bytes, encoder.enctype()).exec(ctx.action);
            }
        }

        if (response == null) {
            return null;
        }

        //2.构建结果
        Result result = new Result(response.code(), response.body().bytes());

        //2.1.设置头
        for (int i = 0, len = response.headers().size(); i < len; i++) {
            result.headerAdd(response.headers().name(i), response.headers().value(i));
        }

        //2.2.设置字符码
        MediaType contentType = response.body().contentType();
        if (contentType != null) {
            result.charsetSet(contentType.charset());
        }

        //3.返回结果
        return result;
    }


    private void pretreatment(Context ctx) {
        if (ctx.config.getDecoder() == null) {
            String at = ctx.config.getHeader(Constants.HEADER_ACCEPT);

            if (at == null) {
                at = ContentTypes.JSON_VALUE;
            }

            ctx.config.setDecoder(NamiManager.getDecoder(at));
        }

        if (ctx.config.getEncoder() == null) {
            String ct = ctx.config.getHeader(Constants.HEADER_CONTENT_TYPE);

            if (ct != null) {
                ctx.config.setEncoder(NamiManager.getEncoder(ct));
            }
        }
    }
}
