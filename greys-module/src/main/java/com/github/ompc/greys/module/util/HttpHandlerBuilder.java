package com.github.ompc.greys.module.util;

import com.github.ompc.greys.module.handler.HttpHandler;
import com.github.ompc.greys.module.handler.impl.LogoHandler;
import com.github.ompc.greys.module.handler.impl.ThanksHandler;
import com.github.ompc.greys.module.handler.impl.VersionHandler;
import com.github.ompc.greys.module.handler.impl.WatchHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP请求处理映射
 */
public class HttpHandlerBuilder {

    private final static Logger logger = LoggerFactory.getLogger(HttpHandlerBuilder.class);

    private static final Map<String, Class<? extends HttpHandler>> httpHandlerMap
            = buildHttpHandlerMap(
            LogoHandler.class,
            ThanksHandler.class,
            VersionHandler.class,
            WatchHandler.class
    );

    private static Map<String, Class<? extends HttpHandler>> buildHttpHandlerMap(final Class<? extends HttpHandler>... httpHandlerClassArray) {
        final Map<String, Class<? extends HttpHandler>> httpHandlerMap
                = new HashMap<String, Class<? extends HttpHandler>>();
        if (ArrayUtils.isNotEmpty(httpHandlerClassArray)) {
            for (final Class<? extends HttpHandler> httpHandlerClass : httpHandlerClassArray) {
                final HttpHandler.Path path = httpHandlerClass.getAnnotation(HttpHandler.Path.class);
                if (null != path) {
                    httpHandlerMap.put("/greys" + path.value(), httpHandlerClass);
                } else {
                    logger.warn("http-handler:{} is not have @Path, ignore this handler.",
                            httpHandlerClass.getName());
                }//if
            }//for
        }//if
        return httpHandlerMap;
    }

    public static class PathNotMappedException extends Exception {
        PathNotMappedException(String path) {
            super(String.format("path:%s is not mapped!", path));
        }
    }

    public static class InitHttpHandlerException extends Exception {
        InitHttpHandlerException(final String path,
                                 final Class<? extends HttpHandler> classOfHttpHandler,
                                 final Throwable cause) {
            super(
                    String.format("init http-handler[path=%s;handler=%s] occur en error:%s",
                            path, classOfHttpHandler.getName(), cause.getMessage()),
                    cause
            );
        }
    }


    /**
     * 映射HttpHandler
     *
     * @param path             HTTP-HANDLER映射路径
     * @param httpParameterMap HTTP请求参数
     * @return 构造好的HttpHandler
     * @throws PathNotMappedException   映射路径不存在Handler对应
     * @throws InitHttpHandlerException 初始化HTTP-HANDLER失败
     */
    public static HttpHandler build(final String path,
                                    final Map<String, String[]> httpParameterMap) throws PathNotMappedException, InitHttpHandlerException {
        final Class<? extends HttpHandler> classOfHttpHandler = httpHandlerMap.get(path);
        if (null != classOfHttpHandler) {
            try {
                return new HttpParameterBinder<HttpHandler>(
                        classOfHttpHandler
                                .getDeclaredConstructor()
                                .newInstance())
                        .binding(httpParameterMap)
                        .build();
            } catch (Throwable cause) {
                throw new InitHttpHandlerException(path, classOfHttpHandler, cause);
            }
        } else {
            throw new PathNotMappedException(path);
        }
    }


}
