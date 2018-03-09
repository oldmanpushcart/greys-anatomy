package com.github.ompc.greys.protocol.impl.v1;

import com.github.ompc.greys.protocol.Gp;
import com.github.ompc.greys.protocol.GpType;
import lombok.Data;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.github.ompc.greys.protocol.util.GpStringUtils.getGaClassName;
import static com.github.ompc.greys.protocol.util.GpStringUtils.getGaClassNames;

@Data
@Gp(GpType.BEHAVIOR_INFO)
public class BehaviorInfo {

    private final String declaringClass;
    private final String name;
    private final int modifier;
    private final String returnType;
    private final List<String> annotationTypes = new ArrayList<String>();
    private final List<String> parameterTypes = new ArrayList<String>();
    private final List<String> exceptionTypes = new ArrayList<String>();

    public BehaviorInfo(final Constructor<?> constructor) {
        this.declaringClass = getGaClassName(constructor.getDeclaringClass());
        this.name = "<init>";
        this.modifier = constructor.getModifiers();
        this.returnType = getGaClassName(constructor.getDeclaringClass());
        this.annotationTypes.addAll(getGaClassNames(constructor.getDeclaredAnnotations()));
        this.parameterTypes.addAll(getGaClassNames(constructor.getParameterTypes()));
        this.exceptionTypes.addAll(getGaClassNames(constructor.getExceptionTypes()));
    }

    public BehaviorInfo(final Method method) {
        this.declaringClass = getGaClassName(method.getDeclaringClass());
        this.name = method.getName();
        this.modifier = method.getModifiers();
        this.returnType = getGaClassName(method.getDeclaringClass());
        this.annotationTypes.addAll(getGaClassNames(method.getDeclaredAnnotations()));
        this.parameterTypes.addAll(getGaClassNames(method.getParameterTypes()));
        this.exceptionTypes.addAll(getGaClassNames(method.getExceptionTypes()));
    }

}
