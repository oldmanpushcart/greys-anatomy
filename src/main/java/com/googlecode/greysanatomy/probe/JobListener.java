package com.googlecode.greysanatomy.probe;

/**
 * 任务监听器
 *
 * @author vlinux
 */
public interface JobListener {

    /**
     * 监听器创建
     */
    void create();

    /**
     * 监听器销毁
     */
    void destroy();
}
