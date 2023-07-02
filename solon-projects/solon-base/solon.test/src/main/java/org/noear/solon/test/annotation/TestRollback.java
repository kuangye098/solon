package org.noear.solon.test.annotation;

import org.noear.solon.data.tran.TranIsolation;
import org.noear.solon.data.tran.TranPolicy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 测试回滚
 *
 * @author noear
 * @since 1.10
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TestRollback {
    /**
     * 事务传导策略
     * */
    TranPolicy policy() default TranPolicy.required;

    /*
     * 事务隔离等级
     * */
    TranIsolation isolation() default TranIsolation.unspecified;

    /**
     * 只读事务
     * */
    boolean readOnly() default false;
}
