package org.noear.solon.socketd.annotation;

import org.noear.solon.core.handle.MethodType;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ClientEndpoint {
    /**
     * 资源描述
     */
    String uri();

    /**
     * 方法
     */
    MethodType method() default MethodType.ALL;

    /**
     * 握手包头
     * */
    String handshakeHeader() default "";

    /**
     * 是否自动重链
     * */
    boolean autoReconnect() default true;

    /**
     * 心跳频率（单位：秒）
     */
    int heartbeatRate() default 30;
}