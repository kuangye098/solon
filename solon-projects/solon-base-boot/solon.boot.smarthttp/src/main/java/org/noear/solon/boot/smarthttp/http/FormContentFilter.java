package org.noear.solon.boot.smarthttp.http;


import org.noear.solon.Utils;
import org.noear.solon.boot.ServerProps;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Handler;

import java.net.URLDecoder;
import java.util.regex.Pattern;

/**
 * 为 DELETE,PATCH 进行表单转码
 *
 * @author noear
 * @since 1.0
 * */
public class FormContentFilter implements Handler {
    private static final Pattern pattern_and =  Pattern.compile("&");
    private static final Pattern pattern_eq =  Pattern.compile("=");

    @Override
    public void handle(Context ctx) throws Throwable {
        String method = ctx.method();

        if ("DELETE".equals(method) || "PATCH".equals(method) || "PUT".equals(method)) {
            parseBodyTry(ctx);
        }
    }

    private void parseBodyTry(Context ctx) throws Exception {
        String ct = ctx.contentType();

        if (ct == null || ctx.paramMap().size() > 0) {
            return;
        }

        if (ct.startsWith("application/x-www-form-urlencoded") == false) {
            return;
        }

        if (Utils.isEmpty(ctx.bodyNew())) {
            return;
        }

        String[] ss = pattern_and.split(ctx.bodyNew()); //ctx.bodyNew().split("&");

        for (String s1 : ss) {
            String[] ss2 = pattern_eq.split(s1); //s1.split("=");

            if (ss2.length == 2) {
                ctx.paramMap().put(ss2[0], URLDecoder.decode(ss2[1], ServerProps.request_encoding));
            }
        }
    }
}
