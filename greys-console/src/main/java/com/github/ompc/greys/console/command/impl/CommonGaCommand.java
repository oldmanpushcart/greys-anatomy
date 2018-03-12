package com.github.ompc.greys.console.command.impl;

import com.github.ompc.greys.console.GaConsoleConfig;
import com.github.ompc.greys.console.command.GaCommands;
import com.github.ompc.greys.console.util.GaURLBuilder;
import com.github.ompc.greys.protocol.GreysProtocol;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;

import java.io.PrintWriter;
import java.net.ConnectException;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;

import static com.github.ompc.greys.protocol.GpSerializer.deserialize;
import static java.lang.String.format;

public abstract class CommonGaCommand implements GaCommands.GaCommand {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private OkHttpClient okHttpClient;
    private PrintWriter writer;

    private final String name = getClass().getAnnotation(Command.class).name();
    private final CountDownLatch waitingWebSocketClosed
            = new CountDownLatch(1);

    private volatile WebSocket webSocket;


    @Override
    final public void execute(OkHttpClient okHttpClient, GaConsoleConfig cfg, PrintWriter writer) throws Throwable {
        this.okHttpClient = okHttpClient;
        this.writer = writer;

        final String URL = buildingURL(new GaURLBuilder(
                format("ws://%s:%s/sandbox/%s/module/websocket/greys/%s",
                        cfg.getIp(),
                        cfg.getPort(),
                        cfg.getNamespace(),
                        getName()
                ),
                Charset.forName("utf-8"))
        );
        logger.info("{} -> URL:{}", this, URL);

        this.webSocket = this.okHttpClient.newWebSocket(
                new Request.Builder()
                        .url(URL)
                        .build(),
                new WebSocketListener() {

                    @Override
                    public void onMessage(WebSocket webSocket, String gpJson) {
                        try {
                            onGp(deserialize(gpJson));
                        } catch (Throwable cause) {
                            logger.warn("{} execute failed.", CommonGaCommand.this, cause);
                            handleError(webSocket, cause, null);
                        }
                    }

                    @Override
                    public void onFailure(WebSocket webSocket, Throwable cause, Response response) {
                        logger.warn("{} onFailure.", CommonGaCommand.this, cause);
                        handleError(webSocket, cause, response);
                    }

                    @Override
                    public void onClosing(WebSocket webSocket, int code, String reason) {
                        logger.info("{} onClosing, code={};reason={};", CommonGaCommand.this, code, reason);
                        handleFinish();
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

    /*
     * 处理错误
     */
    private void handleError(WebSocket webSocket, Throwable cause, Response response) {
        consoleErr("execute failed: %s", getErrorMessage(cause, response));
        closeWebSocketQuietly(webSocket, 1003, cause.getMessage());
        handleFinish();
    }

    /*
     * 优化错误提示文案
     */
    private String getErrorMessage(Throwable cause, Response response) {
        if (null != response) {
            switch (response.code()) {

                /*
                 * jvm-sandbox服务器返回503，说明greys模块不存在
                 */
                case 503:
                    return "maybe greys not loaded!";

            }
        }

        // 发生拒绝链接的异常，说明JVM-SANDBOX都没有加载
        if (cause instanceof ConnectException) {
            return "maybe greys not attach!";
        }

        // 默认返回异常错误信息
        return cause.getMessage();
    }

    /*
     * 处理结束
     */
    private void handleFinish() {
        waitingWebSocketClosed.countDown();
    }


    /**
     * 检查命令状态，必须先执行了 {@link #execute(OkHttpClient, GaConsoleConfig, PrintWriter)}
     */
    private void checkState() {
        if (null == okHttpClient
                || null == writer) {
            throw new IllegalStateException();
        }
    }

    /**
     * 获取控制台输出
     *
     * @return 控制台输出
     */
    final protected PrintWriter getWriter() {
        checkState();
        return writer;
    }

    /**
     * 控制台输出
     *
     * @param format string's format
     * @param args   参数
     */
    final protected void consoleOut(String format, Object... args) {
        getWriter().println(format(format, args));
    }

    /**
     * 控制台输出(错误)
     *
     * @param format string's format
     * @param args   参数
     */
    final protected void consoleErr(String format, Object... args) {
        consoleOut("ERROR: " + format, args);
    }

    /**
     * 构造请求URL
     *
     * @param gaURLBuilder URL构造器
     * @return URL
     */
    protected String buildingURL(GaURLBuilder gaURLBuilder) {
        return gaURLBuilder.build();
    }

    /**
     * 处理协议
     *
     * @param gp GP协议
     * @throws Throwable 处理失败
     */
    abstract protected void onGp(GreysProtocol<?> gp) throws Throwable;

    /**
     * 获取命令名称
     *
     * @return 命令名称
     */
    final public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return format("gaCommand[name=%s;]", getName());
    }

}
