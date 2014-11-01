package com.googlecode.greysanatomy.console.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 精简指令集
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RiscCmd {

    /**
     * 指定命令的名称<br/>
     *
     * @return 返回命令的名称
     */
    public String named();

    /**
     * 指定命令的解释
     *
     * @return
     */
    public String desc();

    /**
     * 排序,在help命令中
     *
     * @return
     */
    public int sort() default 0;

}
