package org.noear.solon.boot.jetty;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.noear.solon.Solon;
import org.noear.solon.SolonApp;
import org.noear.solon.Utils;
import org.noear.solon.boot.ServerLifecycle;
import org.noear.solon.boot.jetty.websocket._SessionManagerImpl;
import org.noear.solon.core.event.EventBus;
import org.noear.solon.core.util.ClassUtil;
import org.noear.solon.socketd.SessionManager;

import java.io.IOException;

class JettyServer extends JettyServerBase implements ServerLifecycle {
    protected Server _server = null;

    @Override
    public void start(String host, int port) throws Throwable {
        setup(Solon.app(), host, port);

        _server.start();
    }

    @Override
    public void stop() throws Throwable {
        if (_server != null) {
            _server.stop();
            _server = null;
        }
    }

    protected void setup(SolonApp app, String host, int port) throws IOException {
        Class<?> wsClz = ClassUtil.loadClass("org.eclipse.jetty.websocket.server.WebSocketHandler");

        QueuedThreadPool threadPool = new QueuedThreadPool(props.getMaxThreads(true), props.getCoreThreads(), (int) props.getIdleTimeout());

        _server = new Server(threadPool);


        //http or https
        _server.addConnector(getConnector(_server, host, port, true));

        //http add
        for (Integer portAdd : addHttpPorts) {
            _server.addConnector(getConnector(_server, host, portAdd, false));
        }

        //session 支持
        if (Solon.app().enableSessionState()) {
            _server.setSessionIdManager(new DefaultSessionIdManager(_server));
        }

        if (app.enableWebSocket() && wsClz != null) {
            _server.setHandler(new HandlerHub(buildHandler()));

            SessionManager.register(new _SessionManagerImpl());
        } else {
            //没有ws包 或 没有开启
            _server.setHandler(buildHandler());
        }

        //1.1:分发事件（充许外部扩展）
        EventBus.push(_server);
    }

    /**
     * 获取Server Handler
     */
    protected Handler buildHandler() throws IOException {
        if (ClassUtil.hasClass(() -> org.eclipse.jetty.servlet.ServletContextHandler.class)) {
            //::走Servlet接口（需要多个包）
            return getServletHandler();
        } else {
            //::走Handler接口（有些功能力会缺失）
            return getJettyHandler();
        }
    }
}
