package com.github.ompc.greys.core.message;

import com.alibaba.fastjson.annotation.JSONField;

import java.io.Serializable;

/**
 * 日志类消息
 * Created by vlinux on 2017/3/1.
 */
public class LogMessage extends GaMessage {

    enum Level {

        DEBUG,
        INFO,
        WARN,
        ERROR

    }

    private final String text;
    private final Level level;


    /**
     * 构建纯文本消息
     *
     * @param text  文本内容
     * @param level 日志等级
     */
    public LogMessage(String text, Level level) {
        super("LOG");
        this.text = text;
        this.level = level;
    }

    public String getText() {
        return text;
    }

    public Level getLevel() {
        return level;
    }

    public static LogMessage debug(String text) {
        return new LogMessage(text, Level.DEBUG);
    }

    public static LogMessage info(String text) {
        return new LogMessage(text, Level.INFO);
    }

    public static LogMessage warn(String text) {
        return new LogMessage(text, Level.WARN);
    }

    public static LogMessage error(String text) {
        return new LogMessage(text, Level.ERROR);
    }

}
