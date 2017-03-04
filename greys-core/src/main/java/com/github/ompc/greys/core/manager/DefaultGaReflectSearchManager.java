package com.github.ompc.greys.core.manager;

import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.api.resource.LoadedClassDataSource;
import com.github.ompc.greys.core.GaMethod;
import com.github.ompc.greys.core.util.GaStringUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 默认{@link GaReflectSearchManager}实现
 * Created by vlinux on 2017/3/2.
 */
public class DefaultGaReflectSearchManager implements GaReflectSearchManager {

    private final LoadedClassDataSource loadedClassDataSource;

    public DefaultGaReflectSearchManager(LoadedClassDataSource loadedClassDataSource) {
        this.loadedClassDataSource = loadedClassDataSource;
    }

    /**
     * 寻找目标类的所有已经被JVM加载的子类
     *
     * @param targetClass 目标类
     * @return 已经被JVM加载的目标类的子类
     */
    private Set<Class<?>> listJvmLoadedSubClass(Class<?> targetClass) {
        final Set<Class<?>> subClassSet = new LinkedHashSet<Class<?>>();
        for (Class<?> subClass : loadedClassDataSource.list()) {
            if (!subClass.equals(targetClass)
                    && targetClass.isAssignableFrom(subClass)) {
                subClassSet.add(subClass);
            }
        }
        return subClassSet;
    }

    /**
     * 通过方法名称模式匹配列出所有匹配的Java类(及其子类)
     *
     * @param patternJavaClassName Java类名称匹配表达式
     * @param isIncludeSubClass    是否包含子类
     * @return 找出所有匹配的Java类(及其子类)
     */
    private Set<Class<?>> listJvmLoadedClasses(final String patternJavaClassName,
                                               final boolean isIncludeSubClass) {
        final Set<Class<?>> foundJvmLoadedClassSet = new LinkedHashSet<Class<?>>();
        for (final Class<?> jvmLoadedClass : loadedClassDataSource.find(new Filter() {

            @Override
            public boolean doClassFilter(int access,
                                         String javaClassName,
                                         String superClassTypeJavaClassName,
                                         String[] interfaceTypeJavaClassNameArray,
                                         String[] annotationTypeJavaClassNameArray) {
                return GaStringUtils.matching(javaClassName, patternJavaClassName);
            }

            @Override
            public boolean doMethodFilter(int access,
                                          String javaMethodName,
                                          String[] parameterTypeJavaClassNameArray,
                                          String[] throwsTypeJavaClassNameArray,
                                          String[] annotationTypeJavaClassNameArray) {
                return true;
            }

        })) {
            foundJvmLoadedClassSet.add(jvmLoadedClass);
            // 如果需要包含子类，则需要增加一次查询
            if (isIncludeSubClass) {
                foundJvmLoadedClassSet.addAll(listJvmLoadedSubClass(jvmLoadedClass));
            }
        }
        return foundJvmLoadedClassSet;
    }

    /**
     * 获取目标类的父类
     * 因为Java的类继承关系是单父类的，所以按照层次排序
     *
     * @param targetClass 目标类
     * @return 目标类的父类列表(顺序按照类继承顺序倒序)
     */
    private ArrayList<Class<?>> recGetSuperClass(Class<?> targetClass) {

        final ArrayList<Class<?>> superClassList = new ArrayList<Class<?>>();
        Class<?> currentClass = targetClass;
        do {
            final Class<?> superClass = currentClass.getSuperclass();
            if (null == superClass) {
                break;
            }
            superClassList.add(currentClass = superClass);
        } while (true);
        return superClassList;

    }

    /**
     * 返回类中的所有可见方法<br/>
     * 所谓可见方法的定义是开发在类中可以直接通过Java语法继承关系感知到的方法
     *
     * @param clazz 目标类
     * @return 类的所有可见方法
     */
    private Set<Method> listVisualMethod(final Class<?> clazz) {
        final Set<Method> methodSet = new LinkedHashSet<Method>();

        // 首先查出当前类所声明的所有方法
        final Method[] classDeclaredMethodArray = clazz.getDeclaredMethods();
        if (null != classDeclaredMethodArray) {
            for (Method declaredMethod : classDeclaredMethodArray) {
                methodSet.add(declaredMethod);
            }
        }

        // 查出当前类所有的父类
        final Collection<Class<?>> superClassSet = recGetSuperClass(clazz);

        // 查出所有父类的可见方法
        for (Class<?> superClass : superClassSet) {

            // 如果是Object这种所有类的基类，则都可以过滤掉了
            if (superClass.equals(Object.class)) {
                continue;
            }

            final Method[] superClassDeclaredMethodArray = superClass.getDeclaredMethods();
            if (null == superClassDeclaredMethodArray) {
                continue;
            }

            for (Method superClassDeclaredMethod : superClassDeclaredMethodArray) {

                final int modifier = superClassDeclaredMethod.getModifiers();

                // 私有方法可以过滤掉
                if (Modifier.isPrivate(modifier)) {
                    continue;
                }

                // public & protected 这两种情况是可以通过继承可见
                // 所以放行
                else if (Modifier.isPublic(modifier)
                        || Modifier.isProtected(modifier)) {
                    methodSet.add(superClassDeclaredMethod);
                }

                // 剩下的情况只剩下默认, 默认的范围需要同包才能生效
                else if (null != clazz
                        && null != superClassDeclaredMethod
                        && null != superClassDeclaredMethod.getDeclaringClass()
                        && !ObjectUtils.notEqual(clazz.getPackage(), superClassDeclaredMethod.getDeclaringClass().getPackage())) {
                    methodSet.add(superClassDeclaredMethod);
                }

            }

        }

        return methodSet;
    }

    /**
     * 列出目标类的所有可视的GaMethod<br/>
     * 所谓可见方法的定义是开发在类中可以直接通过Java语法继承关系感知到的方法
     *
     * @param targetClass 目标类
     * @return 可视的GaMethod方法集合
     */
    private Set<GaMethod> listVisualGaMethod(Class<?> targetClass) {

        final Set<GaMethod> gaMethodSet = new LinkedHashSet<GaMethod>();

        for (final Method method : listVisualMethod(targetClass)) {
            gaMethodSet.add(new GaMethod.MethodImpl(method));
        }

        // 因为构造函数不能继承,所以这里就不用像方法这么复杂的做可视化处理了
        for (final Constructor<?> constructor : targetClass.getDeclaredConstructors()) {
            gaMethodSet.add(new GaMethod.ConstructorImpl(constructor));
        }

        return gaMethodSet;
    }

    /**
     * 列出目标类的所有匹配方法名的可见方法
     *
     * @param targetClass           目标类
     * @param patternJavaMethodName 方法名模式匹配表达式
     * @return 目标类的所有匹配方法名的可见方法
     */
    private Set<GaMethod> listVisualGaMethods(final Class<?> targetClass,
                                              final String patternJavaMethodName) {
        final Set<GaMethod> gaMethodSet = new LinkedHashSet<GaMethod>();
        for (GaMethod gaMethod : listVisualGaMethod(targetClass)) {
            if (GaStringUtils.matching(gaMethod.getName(), patternJavaMethodName)) {
                gaMethodSet.add(gaMethod);
            }
        }
        return gaMethodSet;
    }

    @Override
    public Set<GaMethod> listVisualGaMethods(final String patternJavaClassName,
                                             final String patternJavaMethodName,
                                             final boolean isIncludeSubClass) {
        final Set<GaMethod> foundGaMethodSet = new LinkedHashSet<GaMethod>();
        for (final Class<?> foundClass : listJvmLoadedClasses(patternJavaClassName, isIncludeSubClass)) {
            foundGaMethodSet.addAll(listVisualGaMethods(foundClass, patternJavaMethodName));
        }
        return foundGaMethodSet;
    }

}
