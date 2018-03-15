package com.github.ompc.greys.console.command.impl;

import com.github.ompc.greys.console.GaConsoleConfig;
import com.github.ompc.greys.console.command.GaCommands;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.net.ConnectException;

import static java.lang.String.format;

public abstract class BaseCommand implements GaCommands.GaCommand {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String name = getClass().getAnnotation(CommandLine.Command.class).name();
    private OkHttpClient okHttpClient;
    private GaConsoleConfig config;
    private PrintWriter writer;

    @Override
    final public void execute(final OkHttpClient okHttpClient,
                              final GaConsoleConfig config,
                              final PrintWriter writer) {
        this.okHttpClient = okHttpClient;
        this.config = config;
        this.writer = writer;

        try {
            execute();
        } catch (Throwable cause) {
            handleError(cause);
        }

    }

    /**
     * 执行命令
     *
     * @throws Throwable 执行命令失败
     */
    protected abstract void execute() throws Throwable;

    /**
     * 获取命令名称
     *
     * @return 命令名称
     */
    final public String getName() {
        return name;
    }


    /**
     * 检查命令状态，必须先执行了 {@link #execute(OkHttpClient, GaConsoleConfig, PrintWriter)}
     */
    private void checkState() {
        if (null == okHttpClient
                || null == config
                || null == writer) {
            throw new IllegalStateException();
        }
    }

    /**
     * 获取HttpClient
     *
     * @return OkHttpClient
     */
    protected OkHttpClient getHttpClient() {
        checkState();
        return okHttpClient;
    }

    /**
     * 获取终端配置
     *
     * @return 获取配置
     */
    protected GaConsoleConfig getConfig() {
        checkState();
        return config;
    }

    /**
     * 获取控制台输出
     *
     * @return 控制台输出
     */
    protected PrintWriter getWriter() {
        checkState();
        return writer;
    }

    /**
     * 控制台输出
     *
     * @param format string's format
     * @param args   参数
     */
    protected void consoleOut(String format, Object... args) {
        getWriter().println(format(format, args));
        getWriter().flush();
    }

    /**
     * 控制台输出(错误)
     *
     * @param format string's format
     * @param args   参数
     */
    protected void consoleErr(String format, Object... args) {
        consoleOut("ERROR: " + format, args);
    }

    /*
     * 优化错误提示文案
     */
    protected String getErrorMessage(Throwable cause, Response response) {

        // 有HTTP应答，优先以应答内容为准
        if (null != response) {
            switch (response.code()) {

                /*
                 * jvm-sandbox服务器返回404/503，说明greys模块不存在
                 */
                case 404:
                case 503:
                    return "maybe greys not loaded";

                default:
                    return "" + response.code();

            }
        }

        // 没有应答以异常信息为准
        if (null != cause) {
            // 发生拒绝链接的异常，说明JVM-SANDBOX都没有加载
            if (cause instanceof ConnectException) {
                return "connect reject!";
            }

            // 默认返回异常错误信息
            return "" + cause.getMessage();
        }

        // 什么都没有...
        return "UNKNOW ERROR!";

    }

    /**
     * 处理错误
     *
     * @param cause    错误异常
     * @param response 服务器异常返回
     */
    protected void handleError(Throwable cause, Response response) {
        logger.warn("{} execute fail, code={};",
                this,
                null == response
                        ? null
                        : response.code(),
                cause
        );
        consoleErr("execute fail: %s", getErrorMessage(cause, response));
    }

    /**
     * 处理错误
     *
     * @param cause 错误异常
     */
    protected void handleError(Throwable cause) {
        handleError(cause, null);
    }

    /**
     * 处理错误
     *
     * @param response 服务器异常返回
     */
    protected void handleError(Response response) {
        handleError(null, response);
    }

    @Override
    public String toString() {
        return format("gaCommand[name=%s;]", getName());
    }

}
