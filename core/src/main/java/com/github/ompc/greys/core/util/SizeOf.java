package com.github.ompc.greys.core.util;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;



/**
 * 计算一个对象的大小
 * Created by vlinux on 15/12/10.
 */
public class SizeOf {


    public enum HeapType {

        /**
         * 浅引用大小
         */
        SHALLOW,

        /**
         * 深引用大小
         */
        RETAINED

    }

    private final Instrumentation inst;
    private final long size;


    public SizeOf(final Instrumentation inst, final Object object, final HeapType heapType) {

        this.inst = inst;

        if (null == inst
                || null == object) {
            this.size = 0L;
            return;
        }

        switch (heapType) {
            case SHALLOW: {
                this.size = getShallowSize(object);
                break;
            }
            case RETAINED: {
                this.size = getRetainedSize(object);
                break;
            }
            default: {
                this.size = 0;
            }
        }

    }

    /**
     * 计算浅引用
     *
     * @param target 目标对象
     * @return 目标对象在JVM-HEAP中的浅引用对象大小
     */
    private long getShallowSize(final Object target) {
        return inst.getObjectSize(target);
    }


    /**
     * 计算深引用
     *
     * @param target 目标对象
     * @return 目标对象在JVM-HEAP中的深引用对象大小
     */
    private long getRetainedSize(final Object target) {
        long result = getShallowSize(target);
        final IdentityHashMap<Object, Void> references = new IdentityHashMap<Object, Void>();
        references.put(target, null);
        final ArrayDeque<Object> unprocessed = new ArrayDeque<Object>();
        unprocessed.addFirst(target);
        do {
            Object node = unprocessed.removeFirst();
            Class<?> nodeClass = node.getClass();
            if (nodeClass.isArray()) {
                if (node.getClass().getComponentType().isPrimitive()) {
                    continue;
                }
                int length = Array.getLength(node);
                for (int i = 0; i < length; ++i) {
                    Object elem = Array.get(node, i);
                    if (elem == null) {
                        continue;
                    }
                    if (references.containsKey(elem)) {
                        continue;
                    }
                    unprocessed.addFirst(elem);
                    references.put(elem, null);
                    result += getShallowSize(elem);
                }
                continue;
            }
            while (nodeClass != null) {       // traverse up until we hit Object
                for (Field field : nodeClass.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    field.setAccessible(true);
                    try {
                        Class<?> type = field.getType();
                        // primitive types
                        if (type.isPrimitive()) {
                            continue;
                        }
                        // reference types
                        Object value = field.get(node);
                        if (value == null) {
                            continue;
                        }
                        if (references.containsKey(value)) {
                            continue;
                        }
                        if (isSharedFlyweight(value)) {
                            continue;
                        }
                        unprocessed.addFirst(value);
                        references.put(value, null);
                        result += getShallowSize(value);
                    } catch (IllegalArgumentException e) {
                        return -1L;
                    } catch (IllegalAccessException e) {
                        return -2L;
                    }
                }
                nodeClass = nodeClass.getSuperclass();
            }
        } while (!unprocessed.isEmpty());
        return result;
    }

    /**
     * Returns true if this is a well-known shared flyweight.
     * For example, interned Strings, Booleans and Number objects.
     * <p/>
     * thanks to Dr. Heinz Kabutz
     * see http://www.javaspecialists.co.za/archive/Issue142.html
     */
    private static boolean isSharedFlyweight(Object obj) {
        // optimization - all of our flyweights are Comparable
        if (obj instanceof Comparable) {
            if (obj instanceof Enum) {
                return true;
            } else if (obj instanceof String) {
                return (obj == ((String) obj).intern());
            } else if (obj instanceof Boolean) {
                return (obj == Boolean.TRUE || obj == Boolean.FALSE);
            } else if (obj instanceof Integer) {
                return (obj == Integer.valueOf((Integer) obj));
            } else if (obj instanceof Short) {
                return (obj == Short.valueOf((Short) obj));
            } else if (obj instanceof Byte) {
                return (obj == Byte.valueOf((Byte) obj));
            } else if (obj instanceof Long) {
                return (obj == Long.valueOf((Long) obj));
            } else if (obj instanceof Character) {
                return (obj == Character.valueOf((Character) obj));
            }
        }
        return false;
    }


    /**
     * 获取对象大小
     *
     * @return 对象大小
     */
    public long getSize() {
        return size;
    }

    @Override
    public String toString() {
        if (size < 1024) return size + " B";
        int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return String.format("%.1f %sB", (double)size / (1L << (z*10)), " KMGTPE".charAt(z));
    }
}
