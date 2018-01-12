package com.github.ompc.greys.core.http;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * HTTP参数解析
 * 标注的参数将会根据参数名、参数类型进行自动适配
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface Param {

    /**
     * 是否必填参数
     *
     * @return TRUE:必填;FALSE:非必填
     */
    boolean isRequired() default false;

    /**
     * HTTP参数名
     *
     * @return
     */
    String name();

}
