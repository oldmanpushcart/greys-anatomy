package com.github.ompc.greys.core.util.matcher;

import com.github.ompc.greys.core.util.GaMethod;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;

import static com.github.ompc.greys.core.util.GaReflectUtils.DEFAULT_MOD;

/**
 * 方法匹配
 * Created by vlinux on 15/11/1.
 */
public class GaMethodMatcher extends ReflectMatcher<GaMethod> {

    // 方法参数匹配(顺序相关)
    private final List<Matcher<Class<?>>> parameters;

    public GaMethodMatcher(
            final int modifier,
            final Matcher<String> name,
            final List<Matcher<Class<?>>> parameters,
            final Collection<Matcher<Class<? extends Annotation>>> annotations) {
        super(modifier, name, annotations);
        this.parameters = parameters;
    }

    public GaMethodMatcher(final Matcher<String> methodNameMatcher) {
        this(DEFAULT_MOD, methodNameMatcher, null, null);
    }

    @Override
    boolean reflectMatching(GaMethod targetMethod) {
        if (!matchingParameters(targetMethod)) {
            return false;
        }
        return true;
    }

    private boolean matchingParameters(final GaMethod targetMethod) {

        final Class<?>[] targetParameterClassArray = targetMethod.getParameterTypes();

        // 推空保护
        if (null == parameters
                || null == targetParameterClassArray) {
            return true;
        }

        // 参数集合长度和参数匹配集合不匹配，说明参数列表都对不上了
        // 直接返回false
        if (targetParameterClassArray.length != parameters.size()) {
            return false;
        }

        final int length = targetParameterClassArray.length;
        for (int index = 0; index < length; index++) {
            final Matcher<Class<?>> classMatcher = parameters.get(index);
            if (!classMatcher.matching(targetParameterClassArray[index])) {
                return false;
            }
        }
        return true;
    }

    @Override
    int getTargetModifiers(GaMethod target) {
        return target.getModifiers();
    }

    @Override
    String getTargetName(GaMethod target) {
        return target.getName();
    }

    @Override
    Annotation[] getTargetAnnotationArray(GaMethod target) {
        return target.getAnnotations();
    }
}
