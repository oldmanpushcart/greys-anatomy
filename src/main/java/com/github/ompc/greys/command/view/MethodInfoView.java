package com.github.ompc.greys.command.view;

import com.github.ompc.greys.command.view.TableView.ColumnDefine;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static com.github.ompc.greys.command.view.TableView.Align.LEFT;
import static com.github.ompc.greys.command.view.TableView.Align.RIGHT;
import static com.github.ompc.greys.util.StringUtil.*;

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
        return new TableView(new ColumnDefine[]{
                new ColumnDefine(16, false, RIGHT),
                new ColumnDefine(50, false, LEFT)
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
