package com.github.ompc.greys.protocol;

/**
 * 协议类型
 */
public enum GpType {

    /**
     * GREYS的感谢列表
     */
    THANKS,

    /**
     * 文本消息输出
     */
    TEXT,

    /**
     * 中断当前命令
     */
    TERMINATE,

    /**
     * 渲染进度报告
     */
    PROGRESS,

    /**
     * 类结构信息
     */
    CLASS_INFO,

    /**
     * 行为结构信息
     */
    BEHAVIOR_INFO,

    /**
     * 方法内部调用跟踪
     */
    TRACE

}
