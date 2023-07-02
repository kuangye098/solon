package org.noear.solon.auth.interceptor;

import org.noear.solon.auth.AuthStatus;
import org.noear.solon.auth.AuthUtil;
import org.noear.solon.auth.annotation.AuthPermissions;
import org.noear.solon.core.handle.Result;

/**
 * AuthPermissions 注解拦截器
 *
 * @author noear
 * @since 1.3
 */
public class PermissionsInterceptor extends AbstractInterceptor<AuthPermissions> {
    @Override
    public Class<AuthPermissions> type() {
        return AuthPermissions.class;
    }

    @Override
    public Result verify(AuthPermissions anno) throws Exception {
        if (AuthUtil.verifyPermissions(anno.value(), anno.logical())) {
            return Result.succeed();
        } else {
            return AuthStatus.OF_PERMISSIONS.toResult();
        }
    }
}
