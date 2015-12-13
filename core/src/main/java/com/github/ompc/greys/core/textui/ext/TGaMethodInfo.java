package com.github.ompc.greys.core.textui.ext;

import com.github.ompc.greys.core.textui.TComponent;
import com.github.ompc.greys.core.textui.TKv;
import com.github.ompc.greys.core.util.GaMethod;

import java.lang.annotation.Annotation;

import static com.github.ompc.greys.core.util.GaStringUtils.tranClassName;
import static com.github.ompc.greys.core.util.GaStringUtils.tranModifier;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * Java方法信息控件
 * Created by oldmanpushcart@gmail.com on 15/5/9.
 */
public class TGaMethodInfo implements TComponent {

    private final GaMethod gaMethod;

    public TGaMethodInfo(GaMethod gaMethod) {
        this.gaMethod = gaMethod;
    }

    @Override
    public String rendering() {
        return new TKv()
                .add("declaring-class", gaMethod.getDeclaringClass())
                .add("gaMethod-name", gaMethod.getName())
                .add("modifier", tranModifier(gaMethod.getModifiers()))
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
        final Annotation[] annotationArray = gaMethod.getDeclaredAnnotations();

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
        final Class<?>[] paramTypes = gaMethod.getParameterTypes();
        if (null != paramTypes && paramTypes.length > 0) {
            for (Class<?> clazz : paramTypes) {
                paramsSB.append(tranClassName(clazz)).append("\n");
            }
        }
        return paramsSB.toString();
    }

    private String drawReturn() {
        final StringBuilder returnSB = new StringBuilder();
        final Class<?> returnTypeClass = gaMethod.getReturnType();
        returnSB.append(tranClassName(returnTypeClass)).append("\n");
        return returnSB.toString();
    }

    private String drawExceptions() {
        final StringBuilder exceptionSB = new StringBuilder();
        final Class<?>[] exceptionTypes = gaMethod.getExceptionTypes();
        if (null != exceptionTypes && exceptionTypes.length > 0) {
            for (Class<?> clazz : exceptionTypes) {
                exceptionSB.append(tranClassName(clazz)).append("\n");
            }
        }
        return exceptionSB.toString();
    }

}
