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
     * @return 返回命令的解释
     */
    public String desc();

    /**
     * 例子
     *
     * @return 返回命令的例子
     */
    public String[] eg() default {};

    /**
     * 排序,在help命令中
     *
     * @return 返回命令在目录中的排序
     */
    public int sort() default 0;

}
