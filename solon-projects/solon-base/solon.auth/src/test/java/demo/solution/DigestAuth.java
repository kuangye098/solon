package demo.solution;

import org.noear.solon.Utils;
import org.noear.solon.core.handle.Context;

/**
 * @author noear 2021/5/27 created
 */
public class DigestAuth {
    private static final String TYPE = "Digest ";

    public void auth(Context ctx) {
        String auth0 = ctx.header("Authorization");

        if (Utils.isEmpty(auth0) || auth0.startsWith(TYPE) == false) {
            ctx.status(401);
            ctx.headerSet("WWW-Authenticate", "Digest realm...");
            return;
        }

    }
}
