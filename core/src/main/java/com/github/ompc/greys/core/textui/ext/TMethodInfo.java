package com.github.ompc.greys.core.textui.ext;

import com.github.ompc.greys.core.textui.TComponent;
import com.github.ompc.greys.core.textui.TKv;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static com.github.ompc.greys.core.util.GaStringUtils.tranClassName;
import static com.github.ompc.greys.core.util.GaStringUtils.tranModifier;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * Java方法信息控件
 * Created by vlinux on 15/5/9.
 */
public class TMethodInfo implements TComponent {

    private final Method method;

    public TMethodInfo(Method method) {
        this.method = method;
    }

    @Override
    public String rendering() {
        return new TKv()
                .add("declaring-class", method.getDeclaringClass())
                .add("method-name", method.getName())
                .add("modifier", tranModifier(method.getModifiers()))
                .add("annotation", drawAnnotation())
                .add("parameters", drawParameters())
                .add("return", drawReturn())
                .add("exceptions", drawExceptions())
//                .padding(1)
//                .hasBorder(true)
                .rendering();
    }

    private String drawAnnotation() {

        final StringBuilder annotationSB = new StringBuilder();
        final Annotation[] annotationArray = method.getDeclaredAnnotations();

        if (null != annotationArray && annotationArray.length > 0) {
            for (Annotation annotation : annotationArray) {
                annotationSB.append(tranClassName(annotation.annotationType())).append(",");
            }
            if (annotationSB.length() > 0) {
                annotationSB.deleteCharAt(annotationSB.length() - 1);
            }
        } else {
            annotationSB.append(EMPTY);
        }

        return annotationSB.toString();
    }

    private String drawParameters() {
        final StringBuilder paramsSB = new StringBuilder();
        final Class<?>[] paramTypes = method.getParameterTypes();
        if (null != paramTypes && paramTypes.length > 0) {
            for (Class<?> clazz : paramTypes) {
                paramsSB.append(tranClassName(clazz)).append("\n");
            }
        }
        return paramsSB.toString();
    }

    private String drawReturn() {
        final StringBuilder returnSB = new StringBuilder();
        final Class<?> returnTypeClass = method.getReturnType();
        returnSB.append(tranClassName(returnTypeClass)).append("\n");
        return returnSB.toString();
    }

    private String drawExceptions() {
        final StringBuilder exceptionSB = new StringBuilder();
        final Class<?>[] exceptionTypes = method.getExceptionTypes();
        if (null != exceptionTypes && exceptionTypes.length > 0) {
            for (Class<?> clazz : exceptionTypes) {
                exceptionSB.append(tranClassName(clazz)).append("\n");
            }
        }
        return exceptionSB.toString();
    }

}
