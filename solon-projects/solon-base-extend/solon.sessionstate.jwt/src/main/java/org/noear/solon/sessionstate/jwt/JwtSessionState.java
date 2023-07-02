package org.noear.solon.sessionstate.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.noear.solon.Utils;
import org.noear.solon.boot.web.SessionStateBase;
import org.noear.solon.core.handle.Context;

import java.util.Collection;
import java.util.ServiceConfigurationError;

/**
 * @author noear
 * @since 1.3
 */
public class JwtSessionState extends SessionStateBase {

    protected JwtSessionState(Context ctx) {
        super(ctx);
    }

    //
    // session control
    //

    @Override
    public String sessionId() {
        if (SessionProp.session_jwt_allowUseHeader) {
            return "";
        }

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
        return sessionMap.keySet();
    }

    private Claims sessionMap;

    protected Claims sessionMap() {
        if (sessionMap == null) {
            synchronized (this) {
                if (sessionMap == null) {
                    sessionMap = new DefaultClaims(); //先初始化一下，避免异常时进入死循环

                    String sesId = sessionId();
                    String token = jwtGet();

                    if (Utils.isNotEmpty(token) && token.contains(".")) {
                        Claims claims = JwtUtils.parseJwt(token);

                        if(claims != null) {
                            if (SessionProp.session_jwt_allowUseHeader || sesId.equals(claims.getId())) {
                                if (SessionProp.session_jwt_allowExpire) {
                                    if (claims.getExpiration() != null &&
                                            claims.getExpiration().getTime() > System.currentTimeMillis()) {
                                        sessionMap = claims;
                                    }
                                } else {
                                    sessionMap = claims;
                                }
                            }
                        }
                    }

                    sessionToken = null;
                }
            }
        }

        return sessionMap;
    }


    @Override
    public Object sessionGet(String key) {
        return sessionMap().get(key);
    }

    @Override
    public void sessionSet(String key, Object val) {
        if (val == null) {
            sessionRemove(key);
        } else {
            sessionMap().put(key, val);
            sessionToken = null;
        }
    }

    @Override
    public void sessionRemove(String key) {
        sessionMap().remove(key);
        sessionToken = null;
    }

    @Override
    public void sessionClear() {
        sessionMap().clear();
        sessionToken = null;
    }

    @Override
    public void sessionReset() {
        sessionClear();
        sessionChangeId();
    }

    @Override
    public void sessionRefresh() {
        if (SessionProp.session_jwt_allowUseHeader) {
            return;
        }

        sessionIdPush();
    }

    @Override
    public void sessionPublish() {
        if (SessionProp.session_jwt_allowAutoIssue) {
            String token = sessionToken();

            if (Utils.isNotEmpty(token)) {
                jwtSet(token);
            }
        }
    }

    private String sessionToken;
    @Override
    public String sessionToken() {
        if (sessionToken == null) {
            Claims tmp = sessionMap();

            if (tmp != null) {
                if (SessionProp.session_jwt_allowUseHeader && tmp.size() == 0) {
                    sessionToken = "";
                }

                if (sessionToken == null) {
                    String skey = sessionId();

                    if (SessionProp.session_jwt_allowUseHeader || Utils.isNotEmpty(skey)) {
                        tmp.setId(skey);

                        try {
                            if (SessionProp.session_jwt_allowExpire) {
                                sessionToken = JwtUtils.buildJwt(tmp, _expiry * 1000L);
                            } else {
                                sessionToken = JwtUtils.buildJwt(tmp, 0);
                            }
                        } catch (ServiceConfigurationError e) {
                            //服务切换时，可能配置文件无法加载
                            sessionToken = "";
                        }
                    }
                }
            }
        }

        return sessionToken;
    }

    @Override
    public boolean replaceable() {
        return false;
    }


    protected String jwtGet() {
        if (SessionProp.session_jwt_allowUseHeader) {
            return ctx.header(SessionProp.session_jwt_name);
        } else {
            return cookieGet(SessionProp.session_jwt_name);
        }
    }

    protected void jwtSet(String token) {
        if (SessionProp.session_jwt_allowUseHeader) {
            ctx.headerSet(SessionProp.session_jwt_name, token);
        } else {
            cookieSet(SessionProp.session_jwt_name, token);
        }

        ctx.attrSet(SessionProp.session_jwt_name, token);
    }
}
