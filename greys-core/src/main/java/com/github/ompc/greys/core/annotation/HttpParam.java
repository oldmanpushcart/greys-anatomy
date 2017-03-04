package com.github.ompc.greys.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * HTTP请求参数解析
 * Created by vlinux on 2017/3/1.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpParam {

    /**
     * HTTP请求参数名称
     *
     * @return 请求参数名称
     */
    String value();

}
