package org.noear.solon.extend.uapi;

import org.noear.solon.XNav;
import org.noear.solon.XUtil;
import org.noear.solon.annotation.XMapping;
import org.noear.solon.core.*;


/**
 * UAPI导航控制器
 *
 * 1.提供容器，重新组织XAction运行
 * */
public abstract class UApiGateway implements XHandler , XRender {
    private XHandler _def;
    private XNav _nav;

    public UApiGateway() {
        super();

        _nav = new XNav(this.getClass().getAnnotation(XMapping.class)){
            @Override
            public XHandler get(XContext c, String path) {
                return UApiGateway.this.get(c, path);
            }
        };

        _def = (c) -> c.status(404);

        register();
    }

    /**
     * 注册二级代理
     */
    protected abstract void register();


    @Override
    public void handle(XContext c) throws Throwable {
        //转换上下文
        //
        XContext c2 = context(c);
        XContextUtil.currentRemove();
        XContextUtil.currentSet(c2);

        //调用父级处理
        _nav.handle(c2);
    }

    @Override
    public void render(Object obj, XContext c) throws Throwable {
        c.renderReal(obj);
    }

    public <T extends XHandler> void addBefore(Class<T> clz) {
        _nav.before(Aop.get(clz));
    }

    public <T extends XHandler> void addAfter(Class<T> clz) {
        _nav.after(Aop.get(clz));
    }

    /**
     * 添加接口
     */
    public void add(Class<?> clz, boolean remoting) {
        if (clz != null) {
            BeanWrap bw = Aop.wrap(clz);
            bw.remotingSet(remoting);

            add(bw);
        }
    }

    public void add(Class<?> clz) {
        if (clz != null) {
            add(Aop.wrap(clz));
        }
    }

    /**
     * 添加接口
     */
    public void add(BeanWrap bw) {
        if (bw == null) {
            return;
        }

        BeanWebWrap uw = new BeanWebWrap(bw, _nav.mapping());

        uw.load((path, m, h) -> {
            XAction api = null;
            if (h instanceof XAction) {
                api = (XAction) h;

                if (XUtil.isEmpty(api.name())) {
                    _def = api;
                } else {
                    _nav.add(api.name(), api);
                }
            }
        });
    }


    public XHandler get(XContext c, String path) {
        XAction api = (XAction) _nav.get(c, path);

        if (api == null) {
            if (_def != null) {
                try {
                    _def.handle(c);
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }
            }
            c.setHandled(true);
            return _def;
        } else {
            c.attrSet("uapi", api.name());
            return api;
        }
    }

    /**
     * 转换 上下文（关键的地方）
     */
    protected XContext context(XContext ctx) {
        return new UApiContext(ctx, this);
    }
}
