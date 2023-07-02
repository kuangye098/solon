package org.noear.solon.proxy;

import org.noear.solon.core.AopContext;
import org.noear.solon.core.BeanWrap;

import java.lang.reflect.InvocationHandler;

/**
 * @author noear
 * @since 1.6
 */
public class BeanProxy implements BeanWrap.Proxy {
    private static final BeanProxy global = new BeanProxy();

    public static BeanProxy getGlobal() {
        return global;
    }

    InvocationHandler handler;

    private BeanProxy() {
    }

    protected BeanProxy(InvocationHandler handler) {
        this.handler = handler;
    }

    /**
     * 获取代理
     */
    @Override
    public Object getProxy(AopContext context, Object bean) {
        return new BeanInvocationHandler(context, bean, handler).getProxy();
    }
}
