package org.noear.solon.sessionstate.local;

import org.noear.solon.Utils;
import org.noear.solon.boot.web.SessionStateBase;
import org.noear.solon.core.handle.Context;

import java.util.Collection;


/**
 * 它会是个单例，不能有上下文数据
 * */
public class LocalSessionState extends SessionStateBase {

    private static ScheduledStore _store;

    static {
        _store = new ScheduledStore(_expiry);
    }

    protected LocalSessionState(Context ctx) {
        super(ctx);
    }


    //
    // session control
    //

    @Override
    public String sessionId() {
        String _sessionId = ctx.attr("sessionId", null);

        if (_sessionId == null) {
            _sessionId = sessionIdGet(false);
            ctx.attrSet("sessionId", _sessionId);
        }

        return _sessionId;
    }

    @Override
    public String sessionChangeId() {
        sessionIdGet(true);
        ctx.attrSet("sessionId", null);
        return sessionId();
    }

    @Override
    public Collection<String> sessionKeys() {
        return _store.keys();
    }

    @Override
    public Object sessionGet(String key) {
        return _store.get(sessionId(), key);
    }

    @Override
    public void sessionSet(String key, Object val) {
        if (val == null) {
            sessionRemove(key);
        } else {
            _store.put(sessionId(), key, val);
        }
    }

    @Override
    public void sessionRemove(String key) {
        _store.remove(sessionId(), key);
    }

    @Override
    public void sessionClear() {
        _store.clear(sessionId());
    }

    @Override
    public void sessionReset() {
        sessionClear();
        sessionChangeId();
    }

    @Override
    public void sessionRefresh() {
        String sid = sessionIdPush();

        if (Utils.isEmpty(sid) == false) {
            _store.delay(sessionId());
        }
    }


    @Override
    public boolean replaceable() {
        return false;
    }
}
