package com.github.ompc.greys.core.advisor;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.System.currentTimeMillis;

/**
 * 上下文
 * Created by oldmanpushcart@gmail.com on 15/10/5.
 */
public class Context {

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

}
