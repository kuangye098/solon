package org.noear.solon.boot.jetty.websocket;

import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.core.handle.MethodType;
import org.noear.solon.core.message.Session;
import org.noear.solon.core.message.Message;
import org.noear.solon.socketd.ProtocolManager;
import org.noear.solon.socketd.SessionBase;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.*;

public class _SocketServerSession extends SessionBase {
    public static final Map<org.eclipse.jetty.websocket.api.Session, Session> sessions = new HashMap<>();

    public static Session get(org.eclipse.jetty.websocket.api.Session real) {
        Session tmp = sessions.get(real);
        if (tmp == null) {
            synchronized (real) {
                tmp = sessions.get(real);
                if (tmp == null) {
                    tmp = new _SocketServerSession(real);
                    sessions.put(real, tmp);
                }
            }
        }

        return tmp;
    }

    public static void remove(org.eclipse.jetty.websocket.api.Session real) {
        sessions.remove(real);
    }


    private final org.eclipse.jetty.websocket.api.Session real;
    private final String _sessionId = Utils.guid();

    public _SocketServerSession(org.eclipse.jetty.websocket.api.Session real) {
        this.real = real;
    }

    @Override
    public Object real() {
        return real;
    }


    @Override
    public String sessionId() {
        return _sessionId;
    }

    @Override
    public MethodType method() {
        return MethodType.WEBSOCKET;
    }

    private URI _uri;

    @Override
    public URI uri() {
        if (_uri == null) {
            _uri = real.getUpgradeRequest().getRequestURI();
        }

        return _uri;
    }

    private String _path;

    @Override
    public String path() {
        if (_path == null) {
            _path = uri().getPath();
        }

        return _path;
    }

    @Override
    public void sendAsync(String message) {
        if (Solon.app().enableWebSocketD()) {
            ByteBuffer buf = ProtocolManager.encode(Message.wrap(message));
            real.getRemote().sendBytes(buf, _CallbackImpl.instance);
        } else {
            real.getRemote().sendString(message, _CallbackImpl.instance);
        }
    }

    @Override
    public void sendAsync(Message message) {
        //用于打印
        super.send(message);

        if (Solon.app().enableWebSocketD()) {
            ByteBuffer buf = ProtocolManager.encode(message);
            real.getRemote().sendBytes(buf, _CallbackImpl.instance);
        } else {
            if (message.isString()) {
                real.getRemote().sendString(message.bodyAsString(), _CallbackImpl.instance);
            } else {
                ByteBuffer buf = ByteBuffer.wrap(message.body());
                real.getRemote().sendBytes(buf, _CallbackImpl.instance);
            }
        }
    }

    @Override
    public void send(String message) {
        try {
            if (Solon.app().enableWebSocketD()) {
                ByteBuffer buf = ProtocolManager.encode(Message.wrap(message));
                real.getRemote().sendBytes(buf);
            } else {
                real.getRemote().sendString(message);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void send(Message message) {
        super.send(message);

        try {
            if (Solon.app().enableWebSocketD()) {
                ByteBuffer buf = ProtocolManager.encode(message);
                real.getRemote().sendBytes(buf);
            } else {
                if (message.isString()) {
                    real.getRemote().sendString(message.bodyAsString());
                } else {
                    ByteBuffer buf = ByteBuffer.wrap(message.body());
                    real.getRemote().sendBytes(buf);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


    private boolean _open = true;

    @Override
    public void close() throws IOException {
        if(real == null){
            return;
        }

        _open = false; //jetty 的 close 不及时
        real.close();
        sessions.remove(real);
    }

    @Override
    public boolean isValid() {
        if(real == null){
            return false;
        }

        return _open && real.isOpen();
    }

    @Override
    public boolean isSecure() {
        return real.isSecure();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return real.getRemoteAddress();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return real.getLocalAddress();
    }

    private Object attachment;

    @Override
    public void setAttachment(Object obj) {
        attachment = obj;
    }

    @Override
    public <T> T getAttachment() {
        return (T) attachment;
    }

    @Override
    public Collection<Session> getOpenSessions() {
        return Collections.unmodifiableCollection(sessions.values());
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        _SocketServerSession that = (_SocketServerSession) o;
        return Objects.equals(real, that.real);
    }

    @Override
    public int hashCode() {
        return Objects.hash(real);
    }
}
