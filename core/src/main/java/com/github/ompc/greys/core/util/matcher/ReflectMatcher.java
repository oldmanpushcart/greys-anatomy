package com.github.ompc.greys.core.util.matcher;

import java.lang.annotation.Annotation;
import java.util.Collection;

import static com.github.ompc.greys.core.util.GaReflectUtils.DEFAULT_MOD;

/**
 * 反射相关匹配器
 * Created by vlinux on 15/11/1.
 */
public abstract class ReflectMatcher<T> implements Matcher<T> {

    // 访问修饰符
    private final int modifier;

    // 名称匹配
    private final Matcher<String> name;

    // 声明Annotation匹配
    private final Collection<Matcher<Class<? extends Annotation>>> annotations;

    /**
     * 构造反射操作相关匹配器
     *
     * @param modifier    访问修饰符 具体枚举类型，参考 {@link ReflectMatcher}
     * @param name        匹配名称
     * @param annotations 声名的Annotation匹配器
     */
    public ReflectMatcher(
            final int modifier,
            final Matcher<String> name,
            final Collection<Matcher<Class<? extends Annotation>>> annotations) {
        this.modifier = modifier;
        this.name = name;
        this.annotations = annotations;
    }

    @Override
    final public boolean matching(T target) {

        // 推空保护
        if (null == target) {
            return false;
        }

        // 匹配mod
        if (!matchingModifier(getTargetModifiers(target))) {
            return false;
        }

        // 匹配名称
        if (!matchingName(getTargetName(target))) {
            return false;
        }

        // 匹配Annotation
        if (!matchingAnnotation(getTargetAnnotationArray(target))) {
            return false;
        }

        // 执行目标实现类的比对
        return reflectMatching(target);
    }

    /**
     * @param target 匹配目标
     * @return 匹配结果
     */
    abstract boolean reflectMatching(T target);

    /**
     * 根据实现类的不同，获取目标的访问修饰符
     *
     * @param target 匹配目标
     * @return 匹配目标访问修饰符
     */
    abstract int getTargetModifiers(T target);

    /**
     * 根据实现类的不同，获取匹配目标的名称
     *
     * @param target 匹配目标
     * @return 匹配目标的名称
     */
    abstract String getTargetName(T target);

    /**
     * 根据实现类的不同，获取匹配目标所声明的Annotation数组
     *
     * @param target 匹配目标
     * @return 匹配目标所声明的Annotation数组
     */
    abstract Annotation[] getTargetAnnotationArray(T target);

    private boolean matchingModifier(int targetModifier) {
        // 如果默认就是全匹配，就不用比了
        if (modifier == DEFAULT_MOD) {
            return true;
        }
        return (modifier & targetModifier) != 0;
    }

    private boolean matchingName(String className) {
        return name.matching(className);
    }

    private boolean matchingAnnotation(Annotation[] targetAnnotationArray) {

        // 推空保护，如果匹配器为空则认为放弃对Annotation的匹配需求
        if (null == annotations
                || annotations.isEmpty()) {
            return true;
        }

        // 推空保护，如果目标类没有声明Annotation
        // 而匹配器要求对Annotation进行匹配要求，则直接认为不符合匹配需求
        if (null == targetAnnotationArray) {
            return false;
        }

        // 对传入的Annotation匹配需求进行严格的逐个匹配
        // 只要有一个不匹配则认为匹配失败
        MATCHING_LOOP:
        for (Matcher<Class<? extends Annotation>> matcher : annotations) {

            for (Annotation targetAnnotation : targetAnnotationArray) {
                if (matcher.matching(targetAnnotation.getClass())) {
                    // 只要匹配上一个，则跳过匹配循环
                    continue MATCHING_LOOP;
                }
            }

            // 能走到这一步，说明当前一个都没有匹配上
            return false;
        }

        // 经过上边恶毒的循环之后，没有被拦下，说明符合对Annotation的匹配要求
        return true;

    }

}
