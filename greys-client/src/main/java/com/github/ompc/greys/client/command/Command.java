package com.github.ompc.greys.client.command;

import java.util.*;

/**
 * 命令对象
 */
public final class Command {

    // 命令名称
    private String name;

    // 命令简介
    private String summary;

    // 简单例子
    private String example;

    // 对应HTTP路径
    private String httpPath;

    // 顺序参数列表
    private final List<Param> indexParams = new ArrayList<Param>();

    // 命名参数列表
    private final Map<String, Param> namedParams = new HashMap<String, Param>();




    // GETTER & SETTER

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public String getHttpPath() {
        return httpPath;
    }

    public void setHttpPath(String httpPath) {
        this.httpPath = httpPath;
    }

    public List<Param> getIndexParams() {
        return indexParams;
    }

    public Map<String, Param> getNamedParams() {
        return namedParams;
    }

}
