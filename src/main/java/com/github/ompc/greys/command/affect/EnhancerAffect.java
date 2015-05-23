package com.github.ompc.greys.command.affect;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 增强影响范围<br/>
 * 统计影响类/方法/耗时
 * Created by vlinux on 15/5/19.
 */
public class EnhancerAffect extends Affect {

    private final AtomicInteger cCnt = new AtomicInteger();
    private final AtomicInteger mCnt = new AtomicInteger();

    public EnhancerAffect() {

    }

    public EnhancerAffect(int cCnt, int mCnt) {
        this.cCnt(cCnt);
        this.mCnt(mCnt);
    }

    /**
     * 影响类统计
     *
     * @param cc 类影响计数
     * @return 当前影响类个数
     */
    public int cCnt(int cc) {
        return cCnt.addAndGet(cc);
    }

    /**
     * 影响方法统计
     *
     * @param mc 方法影响计数
     * @return 当前影响方法个数
     */
    public int mCnt(int mc) {
        return mCnt.addAndGet(mc);
    }

    /**
     * 获取影响类个数
     *
     * @return 影响类个数
     */
    public int cCnt() {
        return cCnt.get();
    }

    /**
     * 获取影响方法个数
     *
     * @return 影响方法个数
     */
    public int mCnt() {
        return mCnt.get();
    }

    @Override
    public String toString() {
        return String.format("Affect(class-cnt:%d , method-cnt:%d) in %s ms.",
                cCnt(),
                mCnt(),
                cost());
    }

}
