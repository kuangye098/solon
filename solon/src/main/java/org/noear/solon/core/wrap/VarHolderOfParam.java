package org.noear.solon.core.wrap;

import org.noear.solon.core.AopContext;
import org.noear.solon.core.VarHolder;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;

/**
 * 参数变量容器 临时对象
 *
 * 为了稳藏 Parameter 的一些特性，并统一对外接口
 *
 * @author noear
 * @since 1.0
 * */
public class VarHolderOfParam implements VarHolder {
    private final Parameter p;
    private final ParameterizedType genericType;
    private final AopContext ctx;

    protected Object val;
    protected boolean done;
    protected boolean required = false;
    protected Runnable onDone;

    public VarHolderOfParam(AopContext ctx, Parameter p, Runnable onDone) {
        this.ctx = ctx;
        this.p = p;
        this.onDone = onDone;

        //简化处理 //只在 @Bean 时有用；不会有复杂的泛型
        Type tmp = p.getParameterizedType();
        if (tmp instanceof ParameterizedType) {
            genericType = (ParameterizedType) tmp;
        } else {
            genericType = null;
        }
    }

    @Override
    public AopContext context() {
        return ctx;
    }

    @Override
    public ParameterizedType getGenericType() {
        return genericType;
    }

    @Override
    public boolean isField() {
        return false;
    }

    @Override
    public Class<?> getType() {
        return p.getType();
    }

    @Override
    public Annotation[] getAnnoS() {
        return p.getAnnotations();
    }

    @Override
    public String getFullName() {
        Executable e = p.getDeclaringExecutable();
        return e.getDeclaringClass().getName() + "::" + e.getName();
    }

    @Override
    public void setValue(Object val) {
        this.val = val;
        this.done = true;

        if (onDone != null) {
            onDone.run();
        }
    }

    public Object getValue() {
        return val;
    }

    public boolean isDone() {
        return done;
    }

    @Override
    public boolean required() {
        return required;
    }

    @Override
    public void required(boolean required) {
        this.required = required;
    }
}