package com.github.ompc.greys.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指令命名参数
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NamedArg {

    /**
     * 参数在命令中的命名
     *
     * @return 参数在命令中的命名
     */
    String name();

    /**
     * 参数摘要
     *
     * @return 参数摘要
     */
    String summary() default "";

    /**
     * 更详细的参数注释
     *
     * @return 更详细的参数注释
     */
    String description() default "";

    /**
     * 是否有值
     *
     * @return 是否有值
     */
    boolean hasValue() default false;

}
