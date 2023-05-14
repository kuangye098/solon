package org.noear.solon.core.route;

import org.noear.solon.core.handle.*;
import org.noear.solon.core.message.Listener;
import org.noear.solon.core.message.ListenerHolder;
import org.noear.solon.core.message.Session;

import java.util.*;

/**
 * @author noear
 * @since 1.3
 */
public class RouterDefault implements Router{
    //for handler
    private final RoutingTable<Handler>[] routesH;
    //for listener
    private final RoutingTable<Listener> routesL;

    public RouterDefault() {
        routesH = new RoutingTableDefault[3];

        routesH[0] = new RoutingTableDefault<>();//before:0
        routesH[1] = new RoutingTableDefault<>();//main
        routesH[2] = new RoutingTableDefault<>();//after:2

        routesL = new RoutingTableDefault<>();
    }
    /**
     * 添加路由关系 for Handler
     *
     * @param path 路径
     * @param endpoint 处理点
     * @param method 方法
     * @param index 顺序位
     * @param handler 处理接口
     */
    @Override
    public void add(String path, Endpoint endpoint, MethodType method, int index, Handler handler) {
        RoutingDefault routing = new RoutingDefault<>(path, method, index, handler);

        if (path.contains("*") || path.contains("{")) {
            routesH[endpoint.code].add(routing);
        } else {
            //没有*号的，优先
            routesH[endpoint.code].add(0, routing);
        }
    }

    @Override
    public void remove(String pathPrefix) {
        routesH[Endpoint.before.code].remove(pathPrefix);
        routesH[Endpoint.main.code].remove(pathPrefix);
        routesH[Endpoint.after.code].remove(pathPrefix);

        routesL.remove(pathPrefix);
    }

    /**
     * 获取某个处理点的所有路由记录
     *
     * @param endpoint 处理点
     * @return 处理点的所有路由记录
     * */
    @Override
    public Collection<Routing<Handler>> getAll(Endpoint endpoint){
        return routesH[endpoint.code].getAll();
    }



    /**
     * 区配一个处理（根据上下文）
     *
     * @param ctx 上下文
     * @param endpoint 处理点
     * @return 一个匹配的处理
     */
    @Override
    public Handler matchOne(Context ctx, Endpoint endpoint) {
        String pathNew = ctx.pathNew();
        MethodType method = MethodTypeUtil.valueOf(ctx.method());

        return routesH[endpoint.code].matchOne(pathNew, method);
    }

    /**
     * 区配多个处理（根据上下文）
     *
     * @param ctx 上下文
     * @param endpoint 处理点
     * @return 一批匹配的处理
     */
    @Override
    public List<Handler> matchAll(Context ctx, Endpoint endpoint) {
        String pathNew = ctx.pathNew();
        MethodType method = MethodTypeUtil.valueOf(ctx.method());

        return routesH[endpoint.code].matchAll(pathNew, method);
    }

    /////////////////// for Listener ///////////////////


    /**
     * 添加路由关系 for Listener
     *
     * @param path 路径
     * @param method 方法
     * @param index 顺序位
     * @param listener 监听接口
     */
    @Override
    public void add(String path, MethodType method, int index, Listener listener) {
        Listener lh = new ListenerHolder(path, listener);

        routesL.add(new RoutingDefault<>(path, method, index, lh));
    }

    /**
     * 区配一个目标（根据上下文）
     *
     * @param session 会话对象
     * @return 首个匹配监听
     */
    @Override
    public Listener matchOne(Session session) {
        String path = session.pathNew();

        if (path == null) {
            return null;
        } else {
            return routesL.matchOne(path, session.method());
        }
    }

    @Override
    public List<Listener> matchAll(Session session) {
        String path = session.pathNew();

        if (path == null) {
            return null;
        } else {
            return routesL.matchAll(path, session.method());
        }
    }

    /**
     * 清空路由关系
     */
    @Override
    public void clear() {
        routesH[0].clear();
        routesH[1].clear();
        routesH[2].clear();

        routesL.clear();
    }
}
