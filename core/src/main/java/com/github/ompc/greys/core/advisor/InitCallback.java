package com.github.ompc.greys.core.advisor;

/**
 * 初始化回调
 * Created by oldmanpushcart@gmail.com on 15/10/5.
 */
public interface InitCallback<T> {

    /**
     * 初始化
     *
     * @return 初始化值
     */
    T init();

}

