package com.github.ompc.greys.core.util;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 反射工具类 Created by oldmanpushcart@gmail.com on 15/5/18.
 */
public class GaReflectUtils {

    /**
     * 获取对象某个成员的值
     *
     * @param <T>    T
     * @param target 目标对象
     * @param field  目标属性
     * @return 目标属性值
     * @throws IllegalArgumentException 非法参数
     * @throws IllegalAccessException   非法进入
     */
    public static <T> T getValue(Object target, Field field) throws IllegalArgumentException, IllegalAccessException {
        final boolean isAccessible = field.isAccessible();
        try {
            field.setAccessible(true);
            //noinspection unchecked
            return (T) field.get(target);
        } finally {
            field.setAccessible(isAccessible);
        }
    }

    /**
     * 设置对象某个成员的值
     *
     * @param field  属性对象
     * @param value  属性值
     * @param target 目标对象
     * @throws IllegalArgumentException 非法参数
     * @throws IllegalAccessException   非法进入
     */
    public static void setValue(Field field, Object value, Object target) throws IllegalArgumentException, IllegalAccessException {
        final boolean isAccessible = field.isAccessible();
        try {
            field.setAccessible(true);
            field.set(target, value);
        } finally {
            field.setAccessible(isAccessible);
        }
    }

    /**
     * 获取一个类下的所有成员(包括父类、私有成员)
     *
     * @param clazz 目标类
     * @return 类下所有属性
     */
    public static Set<Field> getFields(Class<?> clazz) {
        final Set<Field> fields = new LinkedHashSet<Field>();
        final Class<?> parentClazz = clazz.getSuperclass();
        Collections.addAll(fields, clazz.getDeclaredFields());
        if (null != parentClazz) {
            fields.addAll(getFields(parentClazz));
        }
        return fields;
    }

    /**
     * 获取一个类下的指定成员
     *
     * @param clazz 目标类
     * @param name  属性名
     * @return 属性
     */
    public static Field getField(Class<?> clazz, String name) {
        for (Field field : getFields(clazz)) {
            if (GaCheckUtils.isEquals(field.getName(), name)) {
                return field;
            }
        }//for
        return null;
    }


    /**
     * 将字符串转换为指定类型，目前只支持9种类型：8种基本类型（包括其包装类）以及字符串
     *
     * @param t     目标对象类型
     * @param value 目标值
     * @return 类型转换后的值
     */
    @SuppressWarnings("unchecked")
    public static <T> T valueOf(Class<T> t, String value) {
        if (GaCheckUtils.isIn(t, int.class, Integer.class)) {
            return (T) Integer.valueOf(value);
        } else if (GaCheckUtils.isIn(t, long.class, Long.class)) {
            return (T) Long.valueOf(value);
        } else if (GaCheckUtils.isIn(t, double.class, Double.class)) {
            return (T) Double.valueOf(value);
        } else if (GaCheckUtils.isIn(t, float.class, Float.class)) {
            return (T) Float.valueOf(value);
        } else if (GaCheckUtils.isIn(t, char.class, Character.class)) {
            return (T) Character.valueOf(value.charAt(0));
        } else if (GaCheckUtils.isIn(t, byte.class, Byte.class)) {
            return (T) Byte.valueOf(value);
        } else if (GaCheckUtils.isIn(t, boolean.class, Boolean.class)) {
            return (T) Boolean.valueOf(value);
        } else if (GaCheckUtils.isIn(t, short.class, Short.class)) {
            return (T) Short.valueOf(value);
        } else if (GaCheckUtils.isIn(t, String.class)) {
            return (T) value;
        } else {
            return null;
        }
    }


    /**
     * 定义类
     *
     * @param targetClassLoader 目标classLoader
     * @param className         类名称
     * @param classByteArray    类字节码数组
     * @return 定义的类
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public static Class<?> defineClass(
            final ClassLoader targetClassLoader,
            final String className,
            final byte[] classByteArray) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        final Method defineClassMethod = ClassLoader.class.getDeclaredMethod(
                "defineClass",
                String.class,
                byte[].class,
                int.class,
                int.class
        );

        synchronized (defineClassMethod) {
            final boolean acc = defineClassMethod.isAccessible();
            try {
                defineClassMethod.setAccessible(true);
                return (Class<?>) defineClassMethod.invoke(
                        targetClassLoader,
                        className,
                        classByteArray,
                        0,
                        classByteArray.length
                );
            } finally {
                defineClassMethod.setAccessible(acc);
            }
        }

    }


//    /**
//     * 获取目标类的ClassLoader<br/>
//     * 因为JVM的ClassLoader采用双亲委派，所以按层次排序
//     *
//     * @param targetClass 目标类
//     * @return ClassLoader层次列表(按层次排序，从近到远)
//     */
//    public static ArrayList<ClassLoader> recGetClassLoader(final Class<?> targetClass) {
//        final ArrayList<ClassLoader> classLoaderList = new ArrayList<ClassLoader>();
//        ClassLoader loader = targetClass.getClassLoader();
//        if (null != loader) {
//            classLoaderList.add(loader);
//            while (true) {
//                loader = loader.getParent();
//                if (null == loader) {
//                    break;
//                }
//                classLoaderList.add(loader);
//            }
//        }
//        return classLoaderList;
//    }

    /**
     * 获取目标类的父类
     * 因为Java的类继承关系是单父类的，所以按照层次排序
     *
     * @param targetClass 目标类
     * @return 目标类的父类列表(顺序按照类继承顺序倒序)
     */
    public static ArrayList<Class<?>> recGetSuperClass(Class<?> targetClass) {

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
     * 计算ClassType
     *
     * @param targetClass 目标类
     * @return 计算出的ClassType
     */
    public static int computeClassType(Class<?> targetClass) {
        int type = 0;
        if (targetClass.isAnnotation())
            type |= TYPE_ANNOTATION;
        if (targetClass.isAnonymousClass())
            type |= TYPE_ANONYMOUS;
        if (targetClass.isArray())
            type |= TYPE_ARRAY;
        if (targetClass.isEnum())
            type |= TYPE_ENUM;
        if (targetClass.isInterface())
            type |= TYPE_INTERFACE;
        if (targetClass.isLocalClass())
            type |= TYPE_LOCAL;
        if (targetClass.isMemberClass())
            type |= TYPE_MEMBER;
        if (targetClass.isPrimitive())
            type |= TYPE_PRIMITIVE;
        if (targetClass.isSynthetic())
            type |= TYPE_SYNTHETIC;
        return type;
    }


    public static final int TYPE_ANNOTATION = 1 << 0;
    public static final int TYPE_ANONYMOUS = 1 << 1;
    public static final int TYPE_ARRAY = 1 << 2;
    public static final int TYPE_ENUM = 1 << 3;
    public static final int TYPE_INTERFACE = 1 << 4;
    public static final int TYPE_LOCAL = 1 << 5;
    public static final int TYPE_MEMBER = 1 << 6;
    public static final int TYPE_PRIMITIVE = 1 << 7;
    public static final int TYPE_SYNTHETIC = 1 << 8;

    /**
     * 默认类型(全匹配)
     */
    public static final int DEFAULT_TYPE =
            TYPE_ANNOTATION
                    | TYPE_ANONYMOUS | TYPE_ARRAY | TYPE_ENUM
                    | TYPE_INTERFACE | TYPE_LOCAL | TYPE_MEMBER
                    | TYPE_PRIMITIVE | TYPE_SYNTHETIC;


    /**
     * 计算类修饰符
     *
     * @param targetClass 目标类
     * @return 类修饰符
     */
    public static int computeModifier(final Class<?> targetClass) {
        return targetClass.getModifiers();
    }

    public static int computeModifier(final Method targetMethod) {
        return targetMethod.getModifiers();
    }

    public static int computeModifier(final Constructor<?> targetConstructor) {
        return targetConstructor.getModifiers();
    }

    public static int computeModifier(final Field targetField) {
        return targetField.getModifiers();
    }

    public static final int MOD_PUBLIC = Modifier.PUBLIC;
    public static final int MOD_PRIVATE = Modifier.PRIVATE;
    public static final int MOD_PROTECTED = Modifier.PROTECTED;
    public static final int MOD_STATIC = Modifier.STATIC;
    public static final int MOD_FINAL = Modifier.FINAL;
    public static final int MOD_SYNCHRONIZED = Modifier.SYNCHRONIZED;
    public static final int MOD_VOLATILE = Modifier.VOLATILE;
    public static final int MOD_TRANSIENT = Modifier.TRANSIENT;
    public static final int MOD_NATIVE = Modifier.NATIVE;
    public static final int MOD_ABSTRACT = Modifier.ABSTRACT;
    public static final int MOD_STRICT = Modifier.STRICT;

    /**
     * 默认匹配修饰符(全匹配)
     */
    public static final int DEFAULT_MOD =
            MOD_FINAL
                    | MOD_PROTECTED | MOD_VOLATILE | MOD_STATIC | MOD_PUBLIC | MOD_SYNCHRONIZED
                    | MOD_TRANSIENT | MOD_ABSTRACT | MOD_NATIVE | MOD_STRICT | MOD_PRIVATE;


}
