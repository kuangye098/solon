package org.noear.solon.core;

import org.noear.solon.Solon;
import org.noear.solon.core.handle.*;

import java.util.*;

/**
 * 内部扩展桥接器
 *
 * <pre><code>
 * //示例：替换 SessionState 服务 (solon.sessionstate.redis: org.noear.solon.sessionstate.redis.XPluginImp.class)
 * public class PluginImp implements Plugin{
 *     @Override
 *     public void start(Solon app) {
 *         //检测 sessionState 是否存在；且优先级是否低于 RedisSessionState
 *         if (Bridge.sessionState() != null
 *                 && Bridge.sessionState().priority() >= RedisSessionState.SESSION_STATE_PRIORITY) {
 *             return;
 *         }
 *
 *         //如果条件满足，则替换掉已有的 sessionState
 *         Bridge.sessionStateSet(RedisSessionState.create());
 *     }
 * }
 *
 * //示例：替换 TranExecutor 服务 (solon.data: org.noear.solon.data.XPluginImp.class)
 * public class PluginImp implements Plugin{
 *     @Override
 *     public void start(Solon app) {
 *         if (app.enableTransaction()) {
 *             //如果有启用事务，则替换 tranExecutor
 *             Bridge.tranExecutorSet(TranExecutorImp.global);
 *         }
 *     }
 * }
 *
 * </code></pre>
 *
 * @author noear
 * @since 1.0
 * */
public class Bridge {
    //
    // SessionState 对接 //与函数同名，_开头
    //
    private static SessionStateFactory _sessionStateFactory = (ctx) -> new SessionStateEmpty();
    private static boolean sessionStateUpdated;

    public static SessionStateFactory sessionStateFactory() {
        return _sessionStateFactory;
    }

    /**
     * 设置Session状态管理器
     */
    public static void sessionStateFactorySet(SessionStateFactory ssf) {
        if (ssf != null) {
            _sessionStateFactory = ssf;

            if (sessionStateUpdated == false) {
                sessionStateUpdated = true;

                Solon.app().before("**", MethodType.HTTP, (c) -> {
                    c.sessionState().sessionRefresh();
                });
            }
        }
    }

    /**
     * 获取Session状态管理器
     */
    public static SessionState sessionState(Context ctx) {
        return _sessionStateFactory.create(ctx);
    }


    //
    // UpstreamFactory 对接
    //
    private static LoadBalance.Factory _upstreamFactory = (g, s) -> null;

    /**
     * 获取负载工厂
     */
    public static LoadBalance.Factory upstreamFactory() {
        return _upstreamFactory;
    }

    /**
     * 设置负载工厂
     */
    public static void upstreamFactorySet(LoadBalance.Factory uf) {
        if (uf != null) {
            _upstreamFactory = uf;
        }
    }
}