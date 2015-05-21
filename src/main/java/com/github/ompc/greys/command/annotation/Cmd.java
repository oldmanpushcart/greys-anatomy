package com.github.ompc.greys.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 指令集
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Cmd {

    /**
     * 指定命令的名称<br/>
     *
     * @return 返回命令的名称
     */
    String named();

    /**
     * 指定命令的解释
     *
     * @return 返回命令的解释
     */
    String desc();

    /**
     * 例子
     *
     * @return 返回命令的例子
     */
    String[] eg() default {};

    /**
     * 排序,在help命令中
     *
     * @return 返回命令在目录中的排序
     */
    int sort() default 0;

}
