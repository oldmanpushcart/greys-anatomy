package com.googlecode.greysanatomy.console.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Arg {

    /**
     * 在命令行中的参数名称
     *
     * @return 参数名称
     */
    public String name();

    /**
     * 是否必填
     *
     * @return
     */
    public boolean isRequired() default true;

    /**
     * 参数注释
     *
     * @return
     */
    public String description() default "";

    /**
     * 参数校验
     *
     * @return
     */
    public ArgVerifier[] verify() default {};

}
