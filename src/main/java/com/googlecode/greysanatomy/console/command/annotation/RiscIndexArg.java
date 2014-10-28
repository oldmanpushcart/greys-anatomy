package com.googlecode.greysanatomy.console.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ����ָ��λ�ò���
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RiscIndexArg {

    /**
     * �����������е�λ��
     * @return
     */
    public int index();

    /**
     * ��������
     * @return
     */
    public String name();

    /**
     * ����ע��
     *
     * @return
     */
    public String description() default "";

    /**
     * ����ϸ�Ĳ���ע��
     * @return
     */
    public String description2() default "";

    /**
     * ����У��
     *
     * @return
     */
    public ArgVerifier[] verify() default {};

    /**
     * �Ƿ����
     *
     * @return
     */
    public boolean isRequired() default true;

}
