package com.github.ompc.greys.client.command;

/**
 * 参数对象
 */
public class Param {

    /**
     * 参数类型
     */
    enum Type {

        // 顺序参数
        INDEX,

        // 命名参数
        NAMED
    }

    // 参数名称
    private String name;

    // 参数类型
    private Type type;

    // 是否必填
    private boolean isRequired = false;

    // 是否有值
    private boolean hasValue = false;

    // 参数简介
    private String summary;

    // 参数详细描述
    private String desc;

    // 对应HTTP参数名
    private String httpName;

    // 对应HTTP参数值
    private String httpValue;


    // GETTER & SETTER

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(boolean required) {
        isRequired = required;
    }

    public boolean isHasValue() {
        return hasValue;
    }

    public void setHasValue(boolean hasValue) {
        this.hasValue = hasValue;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getHttpName() {
        return httpName;
    }

    public void setHttpName(String httpName) {
        this.httpName = httpName;
    }

    public String getHttpValue() {
        return httpValue;
    }

    public void setHttpValue(String httpValue) {
        this.httpValue = httpValue;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
