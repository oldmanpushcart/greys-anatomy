package com.github.ompc.greys.core.advisor;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.System.currentTimeMillis;

/**
 * 上下文
 * Created by vlinux on 15/10/5.
 */
public class Context extends HashMap<String, Object> {

    // 上下文初始化时间
    private final long initTimestamp = currentTimeMillis();

    // 上下文从初始化到关闭所消耗的时间
    private long cost;

    // 上下文关闭标记
    private final AtomicBoolean isCloseRef = new AtomicBoolean(false);

    /**
     * 获取上下文从初始化到关闭所消耗的时间
     *
     * @return 耗时
     */
    public long getCost() {
        return cost;
    }

    /**
     * 关闭上下文
     *
     * @return this
     */
    public Context close() {
        if (!isCloseRef.compareAndSet(false, true)) {
            throw new IllegalStateException("Context already closed.");
        }

        cost = currentTimeMillis() - initTimestamp;
        return this;
    }

    /**
     * 带初始化回调的get<br/>
     * 如果上下文中从未有过该key，则需要重新初始化一次
     *
     * @param key          key
     * @param initCallback init callback
     * @return object
     */
    public <T> T get(String key, InitCallback<T> initCallback) {

        if (!containsKey(key)) {
            final Object object = initCallback.init();
            put(key, object);
            return (T)object;
        } else {
            return (T)get(key);
        }

    }

}
