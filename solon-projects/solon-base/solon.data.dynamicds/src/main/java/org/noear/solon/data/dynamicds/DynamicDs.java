package org.noear.solon.data.dynamicds;

import org.noear.solon.annotation.Around;

import java.lang.annotation.*;

/**
 * 切换动态数据源
 *
 * @author noear
 * @since 1.11
 */
@Around(DynamicDsInterceptor.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DynamicDs {
    String value() default "";
}
