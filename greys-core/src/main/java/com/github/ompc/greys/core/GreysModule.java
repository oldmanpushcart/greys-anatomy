package com.github.ompc.greys.core;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.ModuleLifecycle;
import com.alibaba.jvm.sandbox.api.http.Http;
import com.github.ompc.greys.core.handler.Handler;
import com.github.ompc.greys.core.http.Path;
import com.github.ompc.greys.core.util.HttpParameterParser;
import com.github.ompc.greys.core.util.LogUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Greys模块
 */
@Information(id = "greys", version = "2.0.0.0", author = "oldmanpushcart@gmail.com")
public class GreysModule implements Module, ModuleLifecycle {

    private final Logger logger = LogUtil.getLogger();

    @Override
    public void onLoad() throws Throwable {
        logger.info("greys is loading...");

        // mapping <path : http handler>
        mappingPathHttpHandler();

    }

    @Override
    public void loadCompleted() {
        logger.info("greys load completed.");
    }

    @Override
    public void onUnload() throws Throwable {

    }

    @Override
    public void onActive() throws Throwable {

    }

    @Override
    public void onFrozen() throws Throwable {

    }

    // 命令响应类集合
    private final Map<String/*/CLASS-NAME/METHOD-NAME*/, HttpHandler> mappingOfPathHttpHandlers
            = new HashMap<String, HttpHandler>();

    /**
     * 是否有效的HTTP请求路径
     *
     * @param path HTTP请求路径
     * @return TRUE:有效;FALSE:无效;
     */
    private boolean isEffectivePath(final Path path) {
        return null != path
                && StringUtils.isNotBlank(path.value());
    }

    /**
     * 映射HTTP请求路径和处理器
     */
    private void mappingPathHttpHandler() {
        final Iterator<Handler> cIt = ServiceLoader.load(Handler.class, this.getClass().getClassLoader()).iterator();
        while (cIt.hasNext()) {

            final Handler command = cIt.next();
            final Class<?> classOfCommand = command.getClass();
            final Path pathOnClass = classOfCommand.getAnnotation(Path.class);

            if (!isEffectivePath(pathOnClass)) {
                logger.warn("found {} but @Path on class is invalid, will be ignored this class.", classOfCommand);
                continue;
            }

            for (Method pathMethod : MethodUtils.getMethodsListWithAnnotation(classOfCommand, Path.class)) {
                final Path pathOnMethod = pathMethod.getAnnotation(Path.class);
                if (!isEffectivePath(pathOnMethod)) {
                    logger.warn("found {}#{} but @Path on method is invalid, will be ignored this method.",
                            classOfCommand, pathMethod);
                    continue;
                }
                final String path = "/greys" + pathOnClass.value() + pathOnMethod.value();
                mappingOfPathHttpHandlers.put(path, new HttpHandler(command, pathMethod));

                logger.info("mapping path-handler {}={}#{}, instance is {};",
                        path, classOfCommand.getName(), pathMethod.getName(), command);
            }

        }//while
        logger.info("mapping Path:HttpHandler finished, count={}", mappingOfPathHttpHandlers.size());
    }

    /**
     * 映射HTTP请求处理器
     *
     * @param path http请求路径
     * @return 对应的HTTP请求处理器，如果找不到则返回null
     */
    private HttpHandler mapping(final String path) {
        return mappingOfPathHttpHandlers.get(path);
    }

    /**
     * 解析HttpMethod处理参数
     *
     * @param req        http请求
     * @param resp       http应答
     * @param httpMethod HttpMethod
     * @return HttpMethod所声明的参数数组
     * @throws IOException 在注入JsonPrinter的时候如果遇到网络阻塞，将会抛出该异常
     */
    protected Object[] parseHttpParameters(final HttpServletRequest req,
                                           final HttpServletResponse resp,
                                           final Method httpMethod) throws IOException {

        final Class<?>[] parameterTypeArray = httpMethod.getParameterTypes();
        final Object[] parameterObjectArray = HttpParameterParser.build().parser(httpMethod, req.getParameterMap());
        for (int index = 0; index < parameterTypeArray.length; index++) {

            // inject HttpServletRequest
            if (HttpServletRequest.class.isAssignableFrom(parameterTypeArray[index])) {
                parameterObjectArray[index] = req;
            }

            // inject HttpServletResponse
            else if (HttpServletResponse.class.isAssignableFrom(parameterTypeArray[index])) {
                parameterObjectArray[index] = resp;
            }

            // inject JsonPrinter
            else if (JsonPrinter.class.isAssignableFrom(parameterTypeArray[index])) {
                parameterObjectArray[index] = new JsonPrinter(resp.getWriter());
            }

        }

        return parameterObjectArray;
    }

    /**
     * 按照SERVLET规范，处理HTTP-GET请求
     *
     * @param req  HTTP请求
     * @param resp HTTP应答
     * @throws Throwable 处理HTTP-GET请求失败
     */
    @Http("/*")
    public void onHttpGet(final HttpServletRequest req, final HttpServletResponse resp) throws Throwable {
        final String path = req.getPathInfo();
        logger.debug("handling request[{}] from {};", path, req.getRemoteAddr());

        final HttpHandler httpHandler = mapping(path);

        // 映射命令不粗在，则返回404
        if (null == httpHandler) {
            logger.warn("not found handler to handle path {}", path);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        logger.debug("found Command[{}#{}] handing path {}",
                httpHandler.handler.getClass(), httpHandler.httpMethod.getName(), path);

        // 先设置一个OK，后边有问题再改
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/plain");
        try {
            logger.debug("prepare to handle http request. Command[{}#{}] handing path {}",
                    httpHandler.handler.getClass(), httpHandler.httpMethod.getName(), path);
            httpHandler.httpMethod.invoke(
                    httpHandler.handler,
                    parseHttpParameters(req, resp, httpHandler.httpMethod)
            );
            logger.debug("handle http request finished. Command[{}#{}] handing path {}",
                    httpHandler.handler.getClass(), httpHandler.httpMethod.getName(), path);
        } catch (Throwable cause) {
            logger.warn("handle http request failed. Command[{}#{}] handing path {}",
                    httpHandler.handler.getClass(), httpHandler.httpMethod.getName(), path, cause);
            throw cause;
        }

    }



    /**
     * HTTP请求处理器
     */
    private class HttpHandler {

        // 处理命令对象
        private final Handler handler;

        // 处理命令方法
        private final Method httpMethod;

        HttpHandler(Handler handler, Method httpMethod) {
            this.handler = handler;
            this.httpMethod = httpMethod;
        }

    }

}
