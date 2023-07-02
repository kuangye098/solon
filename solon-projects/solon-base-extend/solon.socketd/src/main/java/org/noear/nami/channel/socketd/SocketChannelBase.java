package org.noear.nami.channel.socketd;

import org.noear.nami.Context;
import org.noear.nami.NamiManager;
import org.noear.nami.common.Constants;
import org.noear.nami.common.ContentTypes;

/**
 * Socket 通道基类
 *
 * @author noear
 * @since 1.2
 */
public class SocketChannelBase  {
    /**
     * 预处理
     *
     * @param ctx 上下文
     * */
    protected void pretreatment(Context ctx) {
        if (ctx.config.getDecoder() == null) {
            String at = ctx.config.getHeader(Constants.HEADER_ACCEPT);

            if (at == null) {
                at = ContentTypes.JSON_VALUE;
            }

            ctx.config.setDecoder(NamiManager.getDecoder(at));
        }

        if (ctx.config.getEncoder() == null) {
            String ct = ctx.config.getHeader(Constants.HEADER_CONTENT_TYPE);

            if (ct == null) {
                ct = ContentTypes.JSON_VALUE;
            }

            ctx.config.setEncoder(NamiManager.getEncoder(ct));
        }
    }
}
