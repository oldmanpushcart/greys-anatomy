package com.github.ompc.greys.core.http;

import java.lang.annotation.*;

/**
 * HTTP渲染路径
 * 可以作用在类和方法上，属于层级递进的关系
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Inherited
public @interface Path {

    /**
     * 路径值
     *
     * @return HTTP路径的值
     */
    String value();

}
