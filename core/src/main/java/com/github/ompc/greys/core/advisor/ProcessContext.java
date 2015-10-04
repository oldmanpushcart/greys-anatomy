package com.github.ompc.greys.core.advisor;

/**
 * 过程上下文
 * Created by vlinux on 15/10/5.
 */
public class ProcessContext extends Context {

    private int deep;

    /**
     * 过程上下文深度自增
     *
     * @return this
     */
    public ProcessContext inc() {
        deep++;
        return this;
    }

    /**
     * 过程上下文深度自减
     *
     * @return this
     */
    public ProcessContext dec() {
        deep--;

        // 如果过程上下文自减到了顶层
        // 则需要关闭上下文
        if (isTop()) {
            close();
        }
        return this;
    }

    /**
     * 是否顶层
     *
     * @return 是否顶层上下文
     */
    public boolean isTop() {
        return deep == 0;
    }

}
