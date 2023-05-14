package org.noear.solon.boot.undertow;

import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.HttpHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.*;
import org.noear.solon.Solon;
import org.noear.solon.SolonApp;
import org.noear.solon.Utils;
import org.noear.solon.boot.ServerConstants;
import org.noear.solon.boot.ServerLifecycle;
import org.noear.solon.boot.ServerProps;
import org.noear.solon.boot.ssl.SslContextFactory;
import org.noear.solon.boot.undertow.http.UtHandlerHandler;
import org.noear.solon.boot.undertow.websocket.UtWsConnectionCallback;
import org.noear.solon.boot.undertow.websocket._SessionManagerImpl;
import org.noear.solon.core.event.EventBus;
import org.noear.solon.socketd.SessionManager;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import static io.undertow.Handlers.websocket;

/**
 * @author  by: Yukai
 * @since : 2019/3/28 15:49
 */
public class UndertowServer extends UndertowServerBase implements ServerLifecycle {
    protected Undertow _server;

    @Override
    public void start(String host, int port) {
        try {
            setup(Solon.app(), host, port);

            _server.start();
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() throws Throwable {
        if (_server != null) {
            _server.stop();
            _server = null;
        }
    }

    protected void setup(SolonApp app, String host, int port) throws Throwable {
        HttpHandler httpHandler = buildHandler();

        //************************** init server start******************
        Undertow.Builder builder = Undertow.builder();

        builder.setServerOption(UndertowOptions.ALWAYS_SET_KEEP_ALIVE, false);

        if (ServerProps.request_maxHeaderSize != 0) {
            builder.setServerOption(UndertowOptions.MAX_HEADER_SIZE, ServerProps.request_maxHeaderSize);
        }

        if (ServerProps.request_maxFileSize != 0) {
            builder.setServerOption(UndertowOptions.MAX_ENTITY_SIZE, (long) ServerProps.request_maxFileSize);
        }

        builder.setIoThreads(props.getCoreThreads());
        if (props.isIoBound()) {
            builder.setWorkerThreads(props.getMaxThreads(true));
        } else {
            builder.setWorkerThreads(props.getMaxThreads(false));
        }


        if (Utils.isEmpty(host)) {
            host = "0.0.0.0";
        }

        if (allowSsl && System.getProperty(ServerConstants.SSL_KEYSTORE) != null) {
            //https
            builder.addHttpsListener(port, host, SslContextFactory.create());
        } else {
            //http
            builder.addHttpListener(port, host);
        }

        //http add
        for(Integer portAdd: addHttpPorts){
            builder.addHttpListener(portAdd, host);
        }

        if (app.enableWebSocket()) {
            builder.setHandler(websocket(new UtWsConnectionCallback(), httpHandler));

            SessionManager.register(new _SessionManagerImpl());
        } else {
            builder.setHandler(httpHandler);
        }


        //1.1:分发事件（充许外部扩展）
        EventBus.push(builder);

        _server = builder.build();

        //************************* init server end********************
    }

    protected HttpHandler buildHandler() throws Exception {
        DeploymentInfo builder = initDeploymentInfo();

        //添加servlet
        builder.addServlet(new ServletInfo("ACTServlet", UtHandlerHandler.class).addMapping("/"));
        //builder.addInnerHandlerChainWrapper(h -> handler); //这个会使过滤器不能使用


        //开始部署
        final ServletContainer container = Servlets.defaultContainer();
        DeploymentManager manager = container.addDeployment(builder);
        manager.deploy();

        return manager.start();
    }
}
