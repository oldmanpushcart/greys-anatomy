package com.github.ompc.greys.core.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Sun Jvm Unsafe holder
 * Created by vlinux on 15/10/6.
 */
public class UnsafeHolder {

    public static final Unsafe unsafe;

    static {

        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            unsafe = (Unsafe) f.get(null);
        } catch (Throwable e) {
            throw new Error(e);
        }

    }

}
