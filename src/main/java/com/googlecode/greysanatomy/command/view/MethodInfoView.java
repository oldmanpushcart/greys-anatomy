package com.googlecode.greysanatomy.command.view;

import com.googlecode.greysanatomy.util.GaStringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static com.googlecode.greysanatomy.util.GaStringUtils.EMPTY;
import static com.googlecode.greysanatomy.util.GaStringUtils.tranClassName;

/**
 * Java方法信息控件
 * Created by vlinux on 15/5/9.
 */
public class MethodInfoView implements View {

    private final Method method;

    public MethodInfoView(Method method) {
        this.method = method;
    }

    @Override
    public String draw() {
        return new KeyValueView()
                .add("method-info", String.format("%s->%s", method.getDeclaringClass().getName(), method.getName()))
                .add("declaring-class", method.getDeclaringClass())
                .add("modifier", GaStringUtils.tranModifier(method.getModifiers()))
                .add("name", method.getName())
                .add("annotation", drawAnnotation())
                .add("parameters", drawParameters())
                .draw();
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

}
