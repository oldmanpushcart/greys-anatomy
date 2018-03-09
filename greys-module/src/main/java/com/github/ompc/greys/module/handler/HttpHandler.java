package com.github.ompc.greys.module.handler;

import java.io.Closeable;
import java.lang.annotation.*;

/**
 * 处理器接口
 * <p>
 * 用于响应GREYS的命令
 */
public interface HttpHandler {

    /**
     * 进行处理
     *
     * @throws Throwable 处理失败
     */
    void onHandle() throws Throwable;

    /**
     * 进行销毁
     */
    void onDestroy();


    /**
     * HTTP渲染路径
     * 可以作用在{@link HttpHandler}上，属于层级递进的关系
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface Path {

        /**
         * 路径值
         *
         * @return HTTP路径的值
         */
        String value();

    }

}
