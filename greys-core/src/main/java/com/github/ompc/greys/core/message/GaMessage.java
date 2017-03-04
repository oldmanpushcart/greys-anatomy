package com.github.ompc.greys.core.message;

/**
 * Greys消息
 */
public class GaMessage {

    /**
     * 消息种类
     * 1.   AFFECT : 影响报告
     * 2. PROGRESS : 进度报告
     * 3.  COMMAND : 命令结果
     * 4.      LOG : 日志类型
     */
    private final String classes;

    /**
     * 构造Greys消息
     *
     * @param classes 消息类型
     */
    public GaMessage(final String classes) {
        this.classes = classes;
    }

    public String getClasses() {
        return classes;
    }
}
