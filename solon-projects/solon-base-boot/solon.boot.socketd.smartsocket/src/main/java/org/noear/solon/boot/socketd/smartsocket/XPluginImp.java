package org.noear.solon.boot.socketd.smartsocket;

import org.noear.solon.Solon;
import org.noear.solon.SolonApp;
import org.noear.solon.Utils;
import org.noear.solon.boot.ServerProps;
import org.noear.solon.boot.prop.impl.SocketServerProps;
import org.noear.solon.core.*;

import org.noear.solon.core.util.LogUtil;
import org.noear.solon.socketd.SessionManager;
import org.noear.solon.socketd.SocketProps;
import org.noear.solon.socketd.client.smartsocket.AioProtocol;
import org.smartboot.socket.transport.AioQuickServer;

public final class XPluginImp implements Plugin {
    private static Signal _signal;

    public static Signal signal() {
        return _signal;
    }

    private AioQuickServer _server = null;

    public static String solon_boot_ver() {
        return "smartsocket-socketd 1.5/" + Solon.version();
    }

    @Override
    public void start(AopContext context) {
        //注册会话管理器
        SessionManager.register(new _SessionManagerImpl());


        if (Solon.app().enableSocketD() == false) {
            return;
        }

        context.lifecycle(-99, () -> {
            start0(Solon.app());
        });
    }

    private void start0(SolonApp app) {
        //初始化属性
        ServerProps.init();

        long time_start = System.currentTimeMillis();


        SocketServerProps props = new SocketServerProps(20000);
        final String _host = props.getHost();
        final int _port = props.getPort();
        final String _name = props.getName();

        try {
            if (Utils.isEmpty(_host)) {
                _server = new AioQuickServer(_port, AioProtocol.instance, new AioServerProcessor());
            } else {
                _server = new AioQuickServer(_host, _port, AioProtocol.instance, new AioServerProcessor());
            }

            _server.setThreadNum(props.getCoreThreads());
            _server.setBannerEnabled(false);
            if (SocketProps.readBufferSize() > 0) {
                _server.setReadBufferSize(SocketProps.readBufferSize());
            }
            if (SocketProps.writeBufferSize() > 0) {
                _server.setWriteBuffer(SocketProps.writeBufferSize(), 16);
            }
            _server.start();

            final String _wrapHost = props.getWrapHost();
            final int _wrapPort = props.getWrapPort();
            _signal = new SignalSim(_name, _wrapHost, _wrapPort, "tcp", SignalType.SOCKET);

            app.signalAdd(_signal);

            long time_end = System.currentTimeMillis();

            LogUtil.global().info("Connector:main: smartsocket-socketd: Started ServerConnector@{[Socket]}{0.0.0.0:" + _port + "}");
            LogUtil.global().info("Server:main: smartsocket-socketd: Started (" + solon_boot_ver() + ") @" + (time_end - time_start) + "ms");
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void stop() throws Throwable {
        if (_server != null) {
            _server.shutdown();
            _server = null;

            LogUtil.global().info("Server:main: smartsocket-socketd: Has Stopped (" + solon_boot_ver() + ")");
        }
    }
}