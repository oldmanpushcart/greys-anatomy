package com.github.ompc.greys.core.util.matcher;

/**
 * 匹配器
 * Created by oldmanpushcart@gmail.com on 15/5/17.
 */
public interface Matcher<T> {

    /**
     * 是否匹配
     *
     * @param target 目标对象
     * @return 目标是否匹配
     */
    boolean matching(T target);

}
