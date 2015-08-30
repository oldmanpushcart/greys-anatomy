package com.github.ompc.greys.core.view;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static com.github.ompc.greys.core.util.GaStringUtils.tranClassName;
import static com.github.ompc.greys.core.util.GaStringUtils.tranModifier;
import static org.apache.commons.lang3.StringUtils.EMPTY;

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
        return new TableView(new TableView.ColumnDefine[]{
                new TableView.ColumnDefine(16, false, TableView.Align.RIGHT),
                new TableView.ColumnDefine(50, false, TableView.Align.LEFT)
        })
                .addRow("declaring-class", method.getDeclaringClass().getName())
                .addRow("method-name", method.getName())
                .addRow("modifier", tranModifier(method.getModifiers()))
                .addRow("annotation", drawAnnotation())
                .addRow("parameters", drawParameters())
                .addRow("return", drawReturn())
                .addRow("exceptions", drawExceptions())
                .padding(1)
                .hasBorder(true)
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
