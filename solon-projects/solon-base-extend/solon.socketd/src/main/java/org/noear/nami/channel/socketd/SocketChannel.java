package org.noear.nami.channel.socketd;


import org.noear.nami.*;
import org.noear.nami.common.Constants;
import org.noear.nami.common.ContentTypes;
import org.noear.solon.Utils;
import org.noear.solon.core.message.Message;
import org.noear.solon.core.message.MessageFlag;
import org.noear.solon.core.message.Session;
import org.noear.solon.socketd.annotation.Handshake;
import org.noear.solon.socketd.util.HeaderUtil;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Socket 通道
 *
 * @author noear
 * @since 1.2
 */
public class SocketChannel extends SocketChannelBase implements Channel {
    public Supplier<Session> sessions;

    public SocketChannel(Supplier<Session> sessions) {
        this.sessions = sessions;
    }

    /**
     * 调用
     *
     * @param ctx 上下文
     * @return 调用结果
     * */
    @Override
    public Result call(Context ctx) throws Throwable {
        pretreatment(ctx);

        if(ctx.config.getDecoder() == null){
            throw new IllegalArgumentException("There is no suitable decoder");
        }

        //0.尝试解码器的过滤
        ctx.config.getDecoder().pretreatment(ctx);

        Message message = null;
        String message_key = Message.guid();
        int flag = MessageFlag.message;

        if (ctx.method != null) {
            //是否为握手
            //
            Handshake h = ctx.method.getAnnotation(Handshake.class);
            if (h != null) {
                flag = MessageFlag.handshake;

                if (Utils.isNotEmpty(h.handshakeHeader())) {
                    Map<String, String> headerMap = HeaderUtil.decodeHeaderMap(h.handshakeHeader());
                    ctx.headers.putAll(headerMap);
                }
            }
        }

        //1.确定编码器
        Encoder encoder = ctx.config.getEncoder();
        if(encoder == null){
            encoder = NamiManager.getEncoder(ContentTypes.JSON_VALUE);
        }

        if(encoder == null){
            throw new IllegalArgumentException("There is no suitable encoder");
        }

        //2.构建消息
        ctx.headers.put(Constants.HEADER_CONTENT_TYPE, encoder.enctype());
        byte[] bytes = encoder.encode(ctx.body);
        message = new Message(flag, message_key, ctx.url, HeaderUtil.encodeHeaderMap(ctx.headers), bytes);

        //3.获取会话
        Session session = sessions.get();
        if(ctx.config.getHeartbeat() > 0){
            session.sendHeartbeatAuto(ctx.config.getHeartbeat());
        }

        //4.发送消息
        Message res = session.sendAndResponse(message, ctx.config.getTimeout());

        if (res == null) {
            return null;
        }

        //2.构建结果
        Result result = new Result(200, res.body());

        //2.1.设置头
        if (Utils.isNotEmpty(res.header())) {
            Map<String, String> map = HeaderUtil.decodeHeaderMap(res.header());
            map.forEach((k, v) -> {
                result.headerAdd(k, v);
            });
        }

        //3.返回结果
        return result;
    }
}
