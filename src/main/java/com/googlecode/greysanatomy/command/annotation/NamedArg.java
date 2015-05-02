package com.googlecode.greysanatomy.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 精简指令命名参数
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NamedArg {

    /**
     * 参数在命令中的位置
     *
     * @return 参数在命令中的位置
     */
    String named();

    /**
     * 参数注释
     *
     * @return 参数注释
     */
    String description() default "";

    /**
     * 参数注释2
     *
     * @return 参数注释2
     */
    String description2() default "";

    /**
     * 是否有值
     *
     * @return 是否有值
     */
    boolean hasValue() default false;

//    /**
//     * 参数校验
//     *
//     * @return 参数校验
//     */
//    public ArgVerifier[] verify() default {};

}
