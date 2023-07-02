package org.noear.solon.validation.annotation;

import org.noear.solon.annotation.Note;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author noear
 * @since 1.0
 * */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Email {
    @Note("正则表达式")
    String value() default  "";

    String message() default "";

    /**
     * 校验分组
     * */
    Class<?>[] groups() default {};
}
