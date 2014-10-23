package com.googlecode.greysanatomy.console.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 参数校验
 *
 * @author vlinux
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface ArgVerifier {

    /**
     * 验证用的正则表达式
     *
     * @return
     */
    public String regex();

    /**
     * 验证失败时的错误提示
     *
     * @return
     */
    public String description();

}
