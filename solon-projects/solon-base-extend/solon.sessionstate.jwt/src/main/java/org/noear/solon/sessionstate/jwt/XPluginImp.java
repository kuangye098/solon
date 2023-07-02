package org.noear.solon.sessionstate.jwt;

import org.noear.solon.Solon;
import org.noear.solon.core.AopContext;
import org.noear.solon.core.Bridge;
import org.noear.solon.core.Plugin;
import org.noear.solon.core.util.LogUtil;

/**
 * @author noear
 * @since 1.3
 */
public class XPluginImp implements Plugin {
    @Override
    public void start(AopContext context) {
        if (Solon.app().enableSessionState() == false) {
            return;
        }

        if (Bridge.sessionStateFactory().priority() >= JwtSessionStateFactory.SESSION_STATE_PRIORITY) {
            return;
        }

        SessionProp.init();

        Bridge.sessionStateFactorySet(JwtSessionStateFactory.getInstance());

        LogUtil.global().info("Session: Jwt session state plugin is loaded");
    }
}
