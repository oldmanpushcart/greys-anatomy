package com.github.ompc.greys.core.util;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * 类搜索工具
 * Created by vlinux on 15/5/17.
 */
public class SearchUtils {

    /**
     * 根据类名匹配，搜已经被JVM加载的类
     *
     * @param inst             inst
     * @param classNameMatcher 类名匹配
     * @return 匹配的类集合
     */
    public static Set<Class<?>> searchClass(Instrumentation inst, Matcher classNameMatcher) {
        final Set<Class<?>> matches = new HashSet<Class<?>>();
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            if (classNameMatcher.matching(clazz.getName())) {
                matches.add(clazz);
            }
        }
        return matches;
    }

    /**
     * 搜索目标类的子类
     *
     * @param inst     inst
     * @param classSet 当前类集合
     * @return 匹配的子类集合
     */
    public static Set<Class<?>> searchSubClass(Instrumentation inst, Set<Class<?>> classSet) {
        final Set<Class<?>> matches = new HashSet<Class<?>>();
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            for (Class<?> superClass : classSet) {
                if (superClass.isAssignableFrom(clazz)) {
                    matches.add(clazz);
                    break;
                }
            }
        }
        return matches;
    }

    /**
     * 搜索目标类的父类
     *
     * @param clazz 目标类
     * @return 目标类的父类列表(顺序按照类继承顺序倒序)
     */
    public static ArrayList<Class<?>> searchSuperClass(Class<?> clazz) {

        final ArrayList<Class<?>> superClassList = new ArrayList<Class<?>>();
        Class<?> currentClass = clazz;
        do {
            final Class<?> superClass = currentClass.getSuperclass();
            if (null == superClass) {
                break;
            }
            superClassList.add(currentClass = superClass);
        } while (true);
        return superClassList;

    }

}
