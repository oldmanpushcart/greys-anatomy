package com.github.ompc.greys.client.command;

public class Param {

    private String name;
    private boolean isRequired = false;
    private boolean hasValue = false;
    private String desc;
    private String httpName;
    private String httpValue;

    public String getName() {
        return name;
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

}
