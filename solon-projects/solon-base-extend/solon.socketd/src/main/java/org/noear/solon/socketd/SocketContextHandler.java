package org.noear.solon.socketd;

import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.core.event.EventBus;
import org.noear.solon.core.handle.MethodType;
import org.noear.solon.core.message.Message;
import org.noear.solon.core.message.Session;

/**
 * SocketD 上下文处理者
 *
 * @author noear
 * @since 1.0
 * */
public class SocketContextHandler {
    public static final SocketContextHandler instance = new SocketContextHandler();

    public void handle(Session session, Message message) {
        if (message == null) {
            return;
        }

        if(message.getHandled()){
            return;
        }

        if(Solon.app().enableWebSocketMvc() == false) {
            if (session.method() == MethodType.WEBSOCKET) {
                return;
            }
        }

        if(Solon.app().enableSocketMvc() == false) {
            if (session.method() == MethodType.SOCKET) {
                return;
            }
        }

        //没有资源描述的，不进入Handler体系
        if (Utils.isEmpty(message.resourceDescriptor())) {
            return;
        }

        try {
            SocketContext ctx = new SocketContext(session, message);

            Solon.app().tryHandle(ctx);

            if (ctx.getHandled() || ctx.status() != 404) {
                ctx.commit();
            }
        } catch (Throwable e) {
            //context 初始化时，可能会出错
            //
            EventBus.pushTry(e);
        }
    }
}
