package com.github.ompc.greys.core.command;

import com.alibaba.fastjson.JSON;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.filter.Filter;
import com.alibaba.jvm.sandbox.api.http.websocket.TextMessageListener;
import com.alibaba.jvm.sandbox.api.http.websocket.WebSocketConnection;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.github.ompc.greys.core.GaProgress;
import com.github.ompc.greys.core.message.GaMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 命令监听器
 */
public abstract class Command implements TextMessageListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private WebSocketConnection webSocketConnection;

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Override
    public void onMessage(String data) {

    }

    /**
     * 写入应答消息
     *
     * @param gaMessage 应答消息
     * @throws IOException 写入失败，常见的原因可能是网络异常
     */
    protected void write(final GaMessage gaMessage) throws IOException {
        if (null != webSocketConnection
                && webSocketConnection.isOpen()) {
            final String jsonString = JSON.toJSONString(gaMessage, true);
            webSocketConnection.write(jsonString);
        }
    }

    @Override
    final public void onOpen(WebSocketConnection conn) {
        this.webSocketConnection = conn;
        try {
            execute();
        } catch (Throwable throwable) {
            logger.warn("command execute failed.", throwable);
            webSocketConnection.disconnect();
        }
    }

    @Override
    final public void onClose(int closeCode, String message) {
        deleteWatch();
    }

    // 等待删除的WATCH集合
    private final Set<Integer> waitingDeleteWatchSet = new LinkedHashSet<Integer>();

    protected void watch(final Filter filter,
                         final EventListener listener,
                         final Event.Type... eventType) {
        waitingDeleteWatchSet.add(
                moduleEventWatcher.watch(
                        filter,
                        listener,
                        new GaProgress(new GaProgress.ProgressReportCallback() {

                            @Override
                            public void onProgress(GaMessage gaMessage) {
                                try {
                                    write(gaMessage);
                                } catch (IOException e) {
                                    logger.warn("report progress occur I/O error.", e);
                                }
                            }

                        }),
                        eventType
                )
        );
    }

    /**
     * 完成命令
     */
    protected void finish() {
        deleteWatch();
        webSocketConnection.disconnect();
    }

    // 删除所有已经进行的观察
    private void deleteWatch() {
        for (int watchId : waitingDeleteWatchSet) {
            moduleEventWatcher.delete(watchId);
        }
    }

    /**
     * 执行命令
     *
     * @throws Throwable 命令执行失败
     */
    abstract protected void execute() throws Throwable;

}
