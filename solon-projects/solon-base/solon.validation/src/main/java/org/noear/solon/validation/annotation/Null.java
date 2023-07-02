package org.noear.solon.validation.annotation;


import org.noear.solon.annotation.Note;

import java.lang.annotation.*;

/**
 * 不能为null
 *
 * @author noear
 * @since 1.0
 * */
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Null {
    /**
     * param names
     * */
    @Note("param names")
    String[] value() default {};

    String message() default "";

    /**
     * 校验分组
     * */
    Class<?>[] groups() default {};
}
