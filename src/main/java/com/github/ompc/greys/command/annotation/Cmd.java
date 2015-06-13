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
    String name();

    /**
     * 指定命令的解释
     *
     * @return 返回命令的解释
     */
    String summary();

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

    /**
     * 是否hacking命令<br/>
     * hacking命令是给开发人员进行命令调试的隐藏命令
     * 由于不需要让普通用户感知，所以不需要在help命令中展示
     * 也不会对这个命令是否在下个版本进行兼容
     *
     * @return true/false
     */
    boolean isHacking() default false;

}
