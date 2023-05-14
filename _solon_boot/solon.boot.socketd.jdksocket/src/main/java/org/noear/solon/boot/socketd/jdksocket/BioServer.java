package org.noear.solon.boot.socketd.jdksocket;

import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.boot.ServerLifecycle;
import org.noear.solon.core.message.Message;
import org.noear.solon.core.message.Session;
import org.noear.solon.core.util.LogUtil;
import org.noear.solon.socketd.client.jdksocket.BioReceiver;
import org.noear.solon.socketd.client.jdksocket.BioSocketSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

class BioServer implements ServerLifecycle {
    static final Logger log = LoggerFactory.getLogger(BioServer.class);

    private ServerSocket server;
    private ExecutorService executor;

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    private void start0(String host, int port) throws IOException {
        if (Utils.isEmpty(host)) {
            server = new ServerSocket(port);
        } else {
            server = new ServerSocket(port, 50, Inet4Address.getByName(host));
        }


        LogUtil.global().info("Server started, waiting for customer connection...");

        while (true) {
            Socket socket = server.accept();

            try {
                Session session = BioSocketSession.get(socket);
                Solon.app().listener().onOpen(session);

                executor.execute(() -> {
                    execute(session, socket);
                });
            } catch (Throwable e) {
                //todo: 确保监听不死
                log.error(e.getMessage(), e);
                //todo: 直接关闭，让客户端知道出问题了
                close(socket);
            }
        }
    }

    private void close(Socket socket){
        try{
            socket.close();
        }catch (Throwable e){

        }
    }

    private void execute(Session session, Socket socket) {
        while (true) {
            try {
                if (socket.isClosed()) {
                    Solon.app().listener().onClose(session);
                    BioSocketSession.remove(socket);
                    break;
                }

                Message message = BioReceiver.receive(socket);
                if (message != null) {
                    Solon.app().listener().onMessage(session, message);
                }
            } catch (Throwable ex) {
                Solon.app().listener().onError(session, ex);
            }
        }
    }

    @Override
    public void start(String host, int port) throws Throwable {
        new Thread(() -> {
            try {
                start0(host, port);
            } catch (RuntimeException e) {
                throw e;
            } catch (SocketException e) {
                if (e.getMessage().contains("closed") == false) {
                    throw new RuntimeException(e);
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    @Override
    public void stop() {
        if (server == null || server.isClosed()) {
            return;
        }

        try {
            server.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
