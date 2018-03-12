package com.github.ompc.greys.console.render;

/**
 * 渲染器
 * <p>
 * 将渲染目标渲染为终端文本
 *
 * @param <T> 渲染目标
 */
public interface Render<T> {

    /**
     * 渲染
     *
     * @param t
     * @return
     */
    String rendering(T t);

}
