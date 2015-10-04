package com.github.ompc.greys.core;

import java.util.Date;

/**
 * 时间片段
 * Created by vlinux on 15/10/4.
 */
public final class TimeFragment {

    // 时间片段ID
    public final int id;

    // 过程ID
    public final int processId;

    // 通知数据
    public final Advice advice;

    // 记录时间戳
    public final Date gmtCreate;

    // 片段耗时
    public final long cost;

    // 片段堆栈
    public final String stack;

    /**
     * 时间片段构建器
     *
     * @param id        时间片段唯一ID
     * @param processId 时间片段执行过程ID
     * @param advice    时间片段所包含得通知上下文
     * @param gmtCreate 时间片段创建时间
     * @param cost      时间片段执行耗时
     * @param stack     时间片段触发堆栈
     */
    public TimeFragment(int id, int processId, Advice advice, Date gmtCreate, long cost, String stack) {
        this.id = id;
        this.processId = processId;
        this.advice = advice;
        this.gmtCreate = gmtCreate;
        this.cost = cost;
        this.stack = stack;
    }

}
