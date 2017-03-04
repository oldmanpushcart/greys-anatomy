package com.github.ompc.greys.core;

import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.github.ompc.greys.core.message.EnhanceAffectGaMessage;
import com.github.ompc.greys.core.message.ProgressGaMessage;
import com.github.ompc.greys.core.message.GaMessage;

import java.io.IOException;

import static com.github.ompc.greys.core.message.ProgressGaMessage.State.*;

/**
 * Greys实现的进度条
 * Created by vlinux on 2017/3/1.
 */
public class GaProgress implements ModuleEventWatcher.Progress {

    // 进度报告回调
    private final ProgressReportCallback progressReportCb;

    // 总记录数
    private int total;

    /**
     * 构造进度报告
     *
     * @param progressReportCb 进度报告回调
     */
    public GaProgress(ProgressReportCallback progressReportCb) {
        this.progressReportCb = progressReportCb;
    }

    @Override
    final public void begin(int total) {
        this.total = total;
        if (null != progressReportCb) {
            progressReportCb.onProgress(new ProgressGaMessage(BEGIN, 0, total));
        }
    }

    @Override
    final public void progressOnSuccess(Class clazz, int index) {
        if (null != progressReportCb) {
            progressReportCb.onProgress(new ProgressGaMessage(PROCESSING, index, total));
        }
    }

    @Override
    final public void progressOnFailed(Class clazz, int index, Throwable cause) {
        if (null != progressReportCb) {
            progressReportCb.onProgress(new ProgressGaMessage(PROCESSING, index, total));
        }
    }

    @Override
    final public void finish(int cCnt, int mCnt) {
        if (null != progressReportCb) {
            progressReportCb.onProgress(new ProgressGaMessage(FINISH, total, total));
            progressReportCb.onProgress(new EnhanceAffectGaMessage(cCnt, mCnt));
        }
    }

    /**
     * 进度报告回调
     */
    public interface ProgressReportCallback {

        /**
         * 进度进行中
         *
         * @param gaMessage 进度消息
         */
        void onProgress(GaMessage gaMessage);

    }

}