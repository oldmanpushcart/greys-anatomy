package com.github.ompc.greys.console.command.impl;

import com.github.ompc.greys.console.command.GaCommands;
import com.github.ompc.greys.console.util.GaURLBuilder;
import com.github.ompc.greys.protocol.GreysProtocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;

import static com.github.ompc.greys.protocol.GpSerializer.deserialize;
import static java.lang.String.format;

public abstract class GpCommand extends BaseCommand implements GaCommands.GaCommand {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private volatile WebSocket webSocket;

    @Override
    final public void execute() throws Throwable {

        final String URL = buildingURL(new GaURLBuilder(
                format("ws://%s:%s/sandbox/%s/module/websocket/greys/%s",
                        getConfig().getIp(),
                        getConfig().getPort(),
                        getConfig().getNamespace(),
                        getName()
                ),
                Charset.forName("UTF-8"))
        );
        logger.info("{} -> URL:{}", this, URL);

        final CountDownLatch waitingWebSocketClosed = new CountDownLatch(1);
        this.webSocket = getHttpClient().newWebSocket(
                new Request.Builder()
                        .url(URL)
                        .build(),
                new WebSocketListener() {

                    @Override
                    public void onMessage(WebSocket webSocket, String gpJson) {
                        try {
                            onGp(deserialize(gpJson));
                        } catch (Throwable cause) {
                            handleError(cause);
                            closeWebSocketQuietly(webSocket, 1003, cause.getMessage());
                        }
                    }

                    @Override
                    public void onFailure(WebSocket webSocket, Throwable cause, Response response) {
                        handleError(cause, response);
                        closeWebSocketQuietly(webSocket, 1003, cause.getMessage());
                    }

                    @Override
                    public void onClosing(WebSocket webSocket, int code, String reason) {
                        logger.info("{} onClosing, code={};reason={};", GpCommand.this, code, reason);
                        waitingWebSocketClosed.countDown();
                    }

                });

        // 等待WebSocket通讯结束
        waitingWebSocketClosed.await();
    }

    @Override
    final public void terminate() {
        closeWebSocketQuietly(webSocket, 1000, "terminate");
    }

    private static void closeWebSocketQuietly(WebSocket webSocket, int code, String reason) {
        if (null != webSocket) {
            webSocket.close(code, reason);
        }
    }

    /**
     * 处理协议
     *
     * @param gp GP协议
     * @throws Throwable 处理失败
     */
    abstract protected void onGp(GreysProtocol<?> gp) throws Throwable;

    /**
     * 构造请求URL
     *
     * @param gaURLBuilder URL构造器
     * @return URL
     */
    protected String buildingURL(GaURLBuilder gaURLBuilder) {
        return gaURLBuilder.build();
    }

}
