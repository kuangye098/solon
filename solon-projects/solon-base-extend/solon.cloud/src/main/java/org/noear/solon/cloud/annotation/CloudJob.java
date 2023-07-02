package org.noear.solon.cloud.annotation;

import org.noear.solon.annotation.Alias;
import org.noear.solon.annotation.Note;

import java.lang.annotation.*;

/**
 * 云端任务
 *
 * @author noear
 * @since 1.3
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CloudJob {
    /**
     * 名称，支持${xxx}配置
     * */
    @Alias("name")
    String value() default "";

    /**
     * 名称，支持${xxx}配置
     * */
    @Alias("value")
    String name() default "";


    /**
     * 调度表达式（具体由适配框架支持） //或cron：支持7段及时区（秒，分，时，日期ofM，月，星期ofW，年）； 或简配：s，m，h，d
     * */
    String cron7x() default "";

    /**
     * 描述, 支持${xxx}配置
     * */
    String description() default "";
}
