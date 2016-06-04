package com.github.ompc.greys.core.util;

import com.github.ompc.greys.core.exception.UnCaughtException;

/**
 * 懒加载
 * Created by vlinux on 16/6/1.
 */
public abstract class LazyGet<T> {

    private volatile boolean isInit = false;
    private volatile T object;

    abstract protected T initialValue() throws Throwable;

    public T get() {

        if (isInit) {
            return object;
        }

        // lazy get
        try {
            object = initialValue();
            isInit = true;
            return object;
        } catch (Throwable throwable) {
            throw new UnCaughtException(throwable);
        }

    }

}
