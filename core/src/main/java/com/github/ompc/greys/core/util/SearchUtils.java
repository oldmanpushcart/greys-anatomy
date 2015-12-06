package com.github.ompc.greys.core.util;

import java.lang.instrument.Instrumentation;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 类搜索工具
 * Created by oldmanpushcart@gmail.com on 15/5/17.
 */
public class SearchUtils {

    /**
     * 根据类名匹配，搜已经被JVM加载的类
     *
     * @param inst             inst
     * @param classNameMatcher 类名匹配
     * @return 匹配的类集合
     */
    public static LinkedHashSet<Class<?>> searchClass(Instrumentation inst, Matcher classNameMatcher) {
        final LinkedHashSet<Class<?>> matchedSet = new LinkedHashSet<Class<?>>();
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            if (classNameMatcher.matching(clazz.getName())) {
                matchedSet.add(clazz);
            }
        }
        return matchedSet;
    }

    /**
     * 搜索目标类的子类
     *
     * @param inst     inst
     * @param classSet 当前类集合
     * @return 匹配的子类集合
     */
    private static LinkedHashSet<Class<?>> searchSubClass(Instrumentation inst, Set<Class<?>> classSet) {
        final LinkedHashSet<Class<?>> matchedSet = new LinkedHashSet<Class<?>>();
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            for (Class<?> superClass : classSet) {
                if (superClass.isAssignableFrom(clazz)) {
                    matchedSet.add(clazz);
                    break;
                }
            }
        }
        return matchedSet;
    }

    /**
     * 根据类名匹配，搜已经被JVM加载的类及其子类
     * @param inst inst
     * @param classNameMatcher 类名匹配
     * @return 匹配的类集合
     */
    public static LinkedHashSet<Class<?>> searchClassWithSubClass(Instrumentation inst, Matcher classNameMatcher) {
        return searchSubClass(inst, searchClass(inst, classNameMatcher));
    }

}
