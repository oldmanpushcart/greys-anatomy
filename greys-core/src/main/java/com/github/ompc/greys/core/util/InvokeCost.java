package com.github.ompc.greys.core.util;

/**
 * 调用耗时
 * Created by vlinux on 16/6/1.
 */
public class InvokeCost {

    private ThreadLocal<Long> costRef = new ThreadLocal<Long>();

    public long begin() {
        final long timestamp = System.currentTimeMillis();
        costRef.set(timestamp);
        return timestamp;
    }

    public long cost() {
        return System.currentTimeMillis() - costRef.get();
    }

}