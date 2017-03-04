package com.github.ompc.greys.core.message;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * 影响记录数消息
 */
public class RecordAffectGaMessage extends AffectGaMessage {

    private final int rCnt;

    /**
     * 构造影响记录数消息
     *
     * @param rCnt 影响记录数
     */
    public RecordAffectGaMessage(final int rCnt) {
        super("RECORD");
        this.rCnt = rCnt;
    }

    @JSONField(name = "rCnt")
    public int getAffectRecordCount() {
        return rCnt;
    }
}
