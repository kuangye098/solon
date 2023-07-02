package org.noear.nami.annotation;

import org.noear.nami.common.ContentTypes;

import java.lang.annotation.*;

/**
 * 指定参数转为Body
 *
 * @author noear
 * @since 1.2
 * @deprecated 2.3
 */
@Deprecated
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Body {
    String contentType() default ContentTypes.JSON_VALUE;
}
