package com.github.ompc.greys.module;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.http.Http;
import com.alibaba.jvm.sandbox.api.http.websocket.WebSocketAcceptor;
import com.alibaba.jvm.sandbox.api.http.websocket.WebSocketConnection;
import com.alibaba.jvm.sandbox.api.http.websocket.WebSocketConnectionListener;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;
import com.alibaba.jvm.sandbox.api.resource.LoadedClassDataSource;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.github.ompc.greys.module.handler.HttpHandler;
import com.github.ompc.greys.module.resource.AutoReleaseWatcher;
import com.github.ompc.greys.module.resource.GpWriter;
import com.github.ompc.greys.module.util.GaStringUtils;
import com.github.ompc.greys.module.util.HttpHandlerBuilder;
import com.github.ompc.greys.module.util.HttpHandlerBuilder.InitHttpHandlerException;
import com.github.ompc.greys.module.util.HttpHandlerBuilder.PathNotMappedException;
import com.github.ompc.greys.module.util.HttpParameterBinder.BindingRequiredException;
import com.github.ompc.greys.module.util.InjectResourceBuilder;
import com.github.ompc.greys.protocol.GreysProtocol;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;

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
public final class GreysModule implements Module, WebSocketAcceptor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private ConfigInfo cfg;

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    @Resource
    private LoadedClassDataSource loadedClassDataSource;

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


    @Http("/banner")
    public void banner(final HttpServletRequest req,
                       final HttpServletResponse resp) throws IOException {
        final StringBuilder buffer = new StringBuilder()
                .append(GaStringUtils.getLogo()).append("\n")
                .append(format(
                        EMPTY +
                                "\nVERSION : %s" +
                                "\n    PID : %s" +
                                "\n    URI : ws://%s/sandbox/%s/module/websocket/greys/*" +
                                "\n UNSAFE : %s" +
                                "\n AUTHOR : oldmanpushcart@gmail.com" +
                                "\nPOWER BY JVM-SANDBOX(Alibaba OpenSource) %s LGPL-3.0" +
                                "\n",
                        GaStringUtils.getVersion(),
                        req.getParameter("pid"),
                        cfg.getServerAddress(),
                        cfg.getNamespace(),
                        cfg.isEnableUnsafe() ? "ENABLE" : "DISABLE",
                        cfg.getVersion()
                ));
        resp.getWriter().println(buffer.toString());
    }

}
