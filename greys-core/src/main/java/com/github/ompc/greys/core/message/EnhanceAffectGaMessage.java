package com.github.ompc.greys.core.message;

import com.alibaba.fastjson.annotation.JSONField;

/**
 * 增强结果消息
 * Created by vlinux on 2017/3/1.
 */
public class EnhanceAffectGaMessage extends AffectGaMessage {

    private final int cCnt;
    private final int mCnt;

    /**
     * 构造增强结果回馈消息
     *
     * @param cCnt 增强类个数
     * @param mCnt 增强方法个数
     */
    public EnhanceAffectGaMessage(final int cCnt,
                                  final int mCnt) {
        super("ENHANCE");
        this.cCnt = cCnt;
        this.mCnt = mCnt;
    }

    @JSONField(name="cCnt")
    public int getAffectClassCount() {
        return cCnt;
    }

    @JSONField(name="mCnt")
    public int getAffectMethodCount() {
        return mCnt;
    }
}
