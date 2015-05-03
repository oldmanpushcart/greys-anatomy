package com.googlecode.greysanatomy.server;

import com.googlecode.greysanatomy.probe.ProbeJobs;
import com.googlecode.greysanatomy.util.IOUtils;
import com.googlecode.greysanatomy.util.LogUtils;

import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;

/**
 * Greys 服务端会话
 * Created by vlinux on 15/5/2.
 */
public class GaSession {


    private final Logger logger = LogUtils.getLogger();

    // Java进程ID
    private final int javaPid;

    // 会话ID
    private final int sessionId;

    // 会话持续时间(单位毫秒)
    private final long sessionDuration;

    // 会话对应的SocketChannel
    private final SocketChannel socketChannel;

    // 会话字符集
    private Charset charset;

    // 会话所关联的JobId，一个会话只能关联一个JobId
    private Integer currentJobId;

    // 会话最后一次交互时间(触摸时间)
    private volatile long gmtLastTouch;


    // 会话的销毁标记，一旦会话被销毁将无法重新恢复
    private final AtomicBoolean isDestroyRef = new AtomicBoolean(false);

    // 会话的写标记，写打开的会话将会完整的输出currentJobId所对应的内容
    private volatile boolean jobRunning = false;

    public GaSession(int javaPid, int sessionId, long sessionDuration, SocketChannel socketChannel, Charset charset) {
        this.javaPid = javaPid;
        this.sessionId = sessionId;
        this.sessionDuration = sessionDuration;
        this.socketChannel = socketChannel;
        this.charset = charset;
        this.gmtLastTouch = currentTimeMillis();
    }

    /**
     * 销毁会话
     */
    public void destroy() {
        if (!isDestroyRef.compareAndSet(false, true)) {
            throw new IllegalStateException(format("session[%d] already destroyed.", sessionId));
        }

        if (null != currentJobId) {
            ProbeJobs.killJob(currentJobId);
        }

        IOUtils.close(socketChannel);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, format("session[%d] destroyed, currentJobId=%s", sessionId, currentJobId));
        }

    }

    /**
     * 触摸会话<br/>
     * 超时触摸的会话有可能会被超时
     */
    public void touch() {
        gmtLastTouch = currentTimeMillis();
    }

    /**
     * 会话是否到期
     *
     * @return true:会话到期;false:会话尚未到期
     */
    public boolean isExpired() {
        return sessionDuration <= currentTimeMillis() - gmtLastTouch;
    }

    @Override
    public String toString() {
        return format("GaSession[%d]:isExpired=%s;currentJobId=%s;", sessionId, isExpired(), currentJobId);
    }

    public void setCurrentJobId(Integer currentJobId) {
        this.currentJobId = currentJobId;
    }

    /**
     * 标记是否有任务在运行
     * @param isRunning true:任务正在运行;false:没有任务在运行
     */
    public void markJobRunning(boolean isRunning) {
        this.jobRunning = isRunning;
    }

    public int getSessionId() {
        return sessionId;
    }

    public Charset getCharset() {
        return charset;
    }

    public Integer getCurrentJobId() {
        return currentJobId;
    }

    /**
     * 当前是否有任务正在运行
     * @return
     */
    public boolean hasJobRunning() {
        return jobRunning;
    }

    public boolean isDestroy() {
        return isDestroyRef.get();
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public long getSessionDuration() {
        return sessionDuration;
    }

    public int getJavaPid() {
        return javaPid;
    }
}