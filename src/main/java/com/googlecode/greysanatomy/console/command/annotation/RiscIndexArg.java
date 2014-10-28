package com.googlecode.greysanatomy.console.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 精简指令位置参数
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RiscIndexArg {

    /**
     * 参数在命令中的位置
     * @return
     */
    public int index();

    /**
     * 参数名称
     * @return
     */
    public String name();

    /**
     * 参数注释
     *
     * @return
     */
    public String description() default "";

    /**
     * 更详细的参数注释
     * @return
     */
    public String description2() default "";

    /**
     * 参数校验
     *
     * @return
     */
    public ArgVerifier[] verify() default {};

    /**
     * 是否必填
     *
     * @return
     */
    public boolean isRequired() default true;

}
