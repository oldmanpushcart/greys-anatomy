package com.github.ompc.greys.module;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.LoadCompleted;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.http.websocket.TextMessageListener;
import com.alibaba.jvm.sandbox.api.http.websocket.WebSocketAcceptor;
import com.alibaba.jvm.sandbox.api.http.websocket.WebSocketConnection;
import com.alibaba.jvm.sandbox.api.http.websocket.WebSocketConnectionListener;
import com.alibaba.jvm.sandbox.api.resource.LoadedClassDataSource;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.github.ompc.greys.module.handler.HttpHandler;
import com.github.ompc.greys.module.resource.AutoReleaseWatcher;
import com.github.ompc.greys.module.resource.GpWriter;
import com.github.ompc.greys.module.util.HttpHandlerBuilder;
import com.github.ompc.greys.module.util.HttpHandlerBuilder.InitHttpHandlerException;
import com.github.ompc.greys.module.util.HttpHandlerBuilder.PathNotMappedException;
import com.github.ompc.greys.module.util.HttpParameterBinder.BindingRequiredException;
import com.github.ompc.greys.module.util.InjectResourceBuilder;
import com.github.ompc.greys.protocol.GreysProtocol;
import org.apache.commons.io.IOUtils;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;

import static java.lang.String.format;

/**
 * 错误请求处理
 */
class ErrorHandler implements HttpHandler {

    @Resource
    GpWriter gpWriter;

    final String errorMessage;

    ErrorHandler(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public void onHandle() throws Throwable {
        gpWriter.write(errorMessage)
                .close();
    }

    @Override
    public void onDestroy() {

    }

}

@MetaInfServices(Module.class)
@Information(id = "greys", version = "2.0.0.0", author = "oldmanpushcart@gmail.com")
public final class GreysModule implements Module, LoadCompleted, WebSocketAcceptor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Resource
    private LoadedClassDataSource loadedClassDataSource;

    @Override
    public void loadCompleted() {
        initLogback();
    }

    private String handleToString(final HttpServletRequest req,
                                  final HttpHandler handler) {
        return format("HTTP-HANDLER[%s;%s;%s]",
                req.getRemoteAddr(),
                req.getPathInfo(),
                handler.getClass().getSimpleName()
        );
    }

    /*
     * 获取对应的处理器
     */
    private HttpHandler getHttpHandler(final HttpServletRequest req) {

        // 根据路径找到对应的处理器并完成初始化
        try {
            return HttpHandlerBuilder
                    .build(req.getPathInfo(), req.getParameterMap());
        }

        // 处理器初始化失败!
        catch (final InitHttpHandlerException cause) {
            logger.warn("{} in path:{} init handler failed.",
                    req.getRemoteAddr(),
                    req.getPathInfo(),
                    cause
            );
            if (cause.getCause() instanceof BindingRequiredException) {
                return new ErrorHandler(
                        format("param:%s was required!",
                                ((BindingRequiredException) cause.getCause()).getName()
                        )
                );
            } else {
                return new ErrorHandler(
                        format("greys occur en inner error: %s!",
                                cause.getMessage()
                        )
                );
            }
        }

        // 路径没有对应的处理器进行映射
        catch (PathNotMappedException e) {
            logger.info("{} in path:{} not found mapping handler.",
                    req.getRemoteAddr(),
                    req.getPathInfo()
            );
            return new ErrorHandler(
                    format("greys unsupported this path:  %s!",
                            req.getPathInfo()
                    )
            );
        }
    }

    @Override
    public WebSocketConnectionListener onAccept(final HttpServletRequest req,
                                                final String protocol) {

        final HttpHandler httpHandler = getHttpHandler(req);
        final String handleToString = handleToString(req, httpHandler);
        logger.info("{} onAccept!", handleToString);

        return new GpListener() {

            private final AutoReleaseWatcher arWatcher
                    = new AutoReleaseWatcher(moduleEventWatcher);

            private WebSocketConnection conn;

            @Override
            public void onOpen(WebSocketConnection conn) {
                logger.info("{} opened.", handleToString);
                this.conn = conn;
                final GpWriter gpWriter = new GpWriter(conn);
                try {
                    new InjectResourceBuilder<HttpHandler>(httpHandler)
                            .inject(gpWriter)
                            .inject(arWatcher)
                            .inject(loadedClassDataSource)
                            .build()
                            .onHandle();
                } catch (Throwable cause) {
                    logger.warn("{} handling occur en error, connection will be closed.", handleToString, cause);
                    conn.disconnect();
                }
            }

            @Override
            public void onGp(GreysProtocol<?> gp) {
                switch (gp.getType()) {
                    case TERMINATE: {
                        logger.info("{} receive terminate from client, connection will be closed.",
                                handleToString);
                        if (null != conn
                                && conn.isOpen()) {
                            conn.disconnect();
                        }
                    }
                }
            }

            @Override
            public void onClose(int closeCode, String message) {
                logger.info("{} closed. code:{};message:{};", handleToString, closeCode, message);
                arWatcher.delete();
                try {
                    httpHandler.onDestroy();
                } catch (Throwable cause) {
                    logger.info("{} destroy occur en error, but will be ignore.", handleToString, cause);
                }
            }

        };
    }


    /*
     * 初始化Logback日志
     */
    private void initLogback() {
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        final JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(loggerContext);
        loggerContext.reset();
        final InputStream logbackConfigIs = GreysModule.class.getResourceAsStream("/com/github/ompc/greys/module/res/greys-logback.xml");
        try {
            configurator.doConfigure(logbackConfigIs);
        } catch (JoranException e) {
            throw new RuntimeException("load logback config failed, you need restart greys", e);
        } finally {
            IOUtils.closeQuietly(logbackConfigIs);
        }

        logger.info("init logback success");
    }

}
