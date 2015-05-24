package com.github.ompc.greys.server;

import com.github.ompc.greys.advisor.AdviceWeaver;
import com.github.ompc.greys.util.IOUtil;
import com.github.ompc.greys.util.LogUtil;

import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static com.github.ompc.greys.util.StringUtil.*;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.logging.Level.INFO;

/**
 * 服务端会话
 * Created by vlinux on 15/5/2.
 */
public class Session {

    private final Logger logger = LogUtil.getLogger();

    // 会话锁ID序列
    private final static AtomicInteger lockTxSeq = new AtomicInteger();

    // 空锁
    private final static int LOCK_TX_EMPTY = -1;


    private final int javaPid;
    private final int sessionId;
    private final long sessionDuration;
    private final SocketChannel socketChannel;
    private Charset charset;

    // 提示符
    private String prompt = DEFAULT_PROMPT;


    // 会话最后一次交互时间(触摸时间)
    private volatile long gmtLastTouch;


    // 是否被销毁
    private volatile boolean isDestroy = false;

    // 会话锁ID
    private final AtomicInteger lockTx = new AtomicInteger(LOCK_TX_EMPTY);


    // 会话输出阻塞队列
    private final BlockingQueue<String> writeQueue = new LinkedBlockingQueue<String>();

    /**
     * 构建Session
     *
     * @param javaPid         Java进程ID
     * @param sessionId       会话ID
     * @param sessionDuration 会话持续时间(单位毫秒)
     * @param socketChannel   socket
     * @param charset         会话字符集
     */
    public Session(int javaPid, int sessionId, long sessionDuration, SocketChannel socketChannel, Charset charset) {
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

        isDestroy = true;
        IOUtil.close(socketChannel);

        if (logger.isLoggable(INFO)) {
            logger.log(INFO, format("session[%d] destroyed.", sessionId));
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
     * @return true:会话到期 / false:会话尚未到期
     */
    public boolean isExpired() {
        return sessionDuration <= currentTimeMillis() - gmtLastTouch;
    }

    /**
     * 锁定会话
     *
     * @return true : 锁定成功 / false : 锁定失败
     */
    public boolean tryLock() {
        return lockTx.compareAndSet(LOCK_TX_EMPTY, lockTxSeq.getAndIncrement());
    }

    /**
     * 解锁会话
     */
    public void unLock() {

        final int currentLockTx = lockTx.get();
        if (!lockTx.compareAndSet(currentLockTx, LOCK_TX_EMPTY)) {
            // 能到这一步说明是lock()/unLock()编写出错，需要开发排查
            throw new IllegalStateException();
        }

        // 解锁的时候需要清理输出队列
        writeQueue.clear();

        // 取消监听注册
        AdviceWeaver.unReg(currentLockTx);
    }

    /**
     * 当前会话是否已经被锁定
     *
     * @return true : 锁定 / false : 未锁定
     */
    public boolean isLocked() {
        return lockTx.get() != LOCK_TX_EMPTY;
    }

    /**
     * 获取锁
     *
     * @return 返回锁ID
     */
    public int getLock() {
        return lockTx.get();
    }

    /**
     * 当前会话是否已经被销毁
     *
     * @return true : 销毁 / false : 违背销毁
     */
    public boolean isDestroy() {
        return isDestroy;
    }

    /**
     * 获取提示符<br/>
     * 这里主要是要求增加上\r
     *
     * @return 可以正常绘制的提示符
     */
    public String prompt() {
        return isNotBlank(getPrompt())
                ? "\r" + getPrompt()
                : EMPTY;
    }

    /**
     * 获取会话提示符
     *
     * @return 会话提示符
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * 设置会话提示符
     *
     * @param prompt 会话提示符
     */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    /**
     * 获取会话写入队列
     *
     * @return 写入队列
     */
    public BlockingQueue<String> getWriteQueue() {
        return writeQueue;
    }

    public int getSessionId() {
        return sessionId;
    }

    public Charset getCharset() {
        return charset;
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