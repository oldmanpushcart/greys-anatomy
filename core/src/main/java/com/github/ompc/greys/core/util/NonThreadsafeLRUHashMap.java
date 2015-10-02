package com.github.ompc.greys.core.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 非线程安全的带LRU功能的HashMap<br/>
 * Created by vlinux on 15/10/2.
 */
public class NonThreadsafeLRUHashMap<K, V> extends LinkedHashMap<K, V> {

    // LRU缓存空间
    private final int capacity;

    // 加载因子
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    // 访问顺序优先
    private static final boolean DEFAULT_ACCESS_ORDER = true;

    public NonThreadsafeLRUHashMap(int capacity) {
        this(capacity, DEFAULT_LOAD_FACTOR, DEFAULT_ACCESS_ORDER);
    }

    public NonThreadsafeLRUHashMap(int capacity, float loadFactor, boolean accessOrder) {
        super(capacity, loadFactor, accessOrder);
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }

}
