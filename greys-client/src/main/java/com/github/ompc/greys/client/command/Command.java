package com.github.ompc.greys.client.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Command {

    private String name;
    private String summary;
    private String example;
    private String httpPath;

    private final List<Param> indexParams = new ArrayList<Param>();
    private final Map<String,Param> namedParams = new HashMap<String,Param>();

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
