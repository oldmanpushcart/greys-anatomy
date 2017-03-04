package com.github.ompc.greys.core.message;

/**
 * 影响结果消息
 * Created by vlinux on 2017/3/4.
 */
public class AffectGaMessage extends GaMessage {

    // 影响类型
    private final String type;

    public AffectGaMessage(String type) {
        super("AFFECT");
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
