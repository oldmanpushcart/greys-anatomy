package com.github.ompc.greys.core;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.ModuleLifecycle;
import com.alibaba.jvm.sandbox.api.http.websocket.WebSocketAcceptor;
import com.alibaba.jvm.sandbox.api.http.websocket.WebSocketConnectionListener;
import com.alibaba.jvm.sandbox.api.resource.LoadedClassDataSource;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.github.ompc.greys.core.annotation.HttpParam;
import com.github.ompc.greys.core.command.Command;
import com.github.ompc.greys.core.command.WatchCommand;
import com.github.ompc.greys.core.manager.DefaultGaReflectSearchManager;
import com.github.ompc.greys.core.manager.GaReflectSearchManager;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Greys沙箱模块
 * Created by vlinux on 2017/3/1.
 */
@Information(id = "greys", author = "oldmanpushcart@gmail.com", version = "2.0.0.0")
public class GreysModule implements Module, WebSocketAcceptor {

    private static final String PATH_PREFIX = "/greys";

    // 路径命令映射
    private static final Map<String, Class<? extends Command>> pathCommandClassMapping
            = new LinkedHashMap<String, Class<? extends Command>>();

    static {
        pathCommandClassMapping.put("/watch", WatchCommand.class);
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private LoadedClassDataSource loadedClassDataSource;

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    /**
     * 映射命令
     *
     * @param path URL路径
     * @return 路径所映射的命令，如果找不到则返回null
     */
    private Class<? extends Command> mappingCommand(final String path) {
        for (final Map.Entry<String, Class<? extends Command>> entry : pathCommandClassMapping.entrySet()) {
            if (StringUtils.equals(path, PATH_PREFIX + entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean isAssignableFrom(final Class<?> targetClass, final Class<?>... classArray) {
        if (ArrayUtils.isEmpty(classArray)) {
            return false;
        }
        for (final Class<?> clazz : classArray) {
            if (clazz.isAssignableFrom(targetClass)) {
                return true;
            }
        }
        return false;
    }

    // 向命令注入HTTP参数
    private void injectHttpParam(final Command command,
                                 final HttpServletRequest req) throws IllegalAccessException {
        for (final Field field : FieldUtils.getFieldsListWithAnnotation(command.getClass(), HttpParam.class)) {
            final Class<?> fieldType = field.getType();
            final HttpParam httpParam = field.getAnnotation(HttpParam.class);

            // 如果是数组，当前仅支持String[]
            if (fieldType.isArray()
                    && String.class.isAssignableFrom(fieldType.getComponentType())
                    && ArrayUtils.isNotEmpty(req.getParameterValues(httpParam.value()))) {
                FieldUtils.writeField(field, command, req.getParameterValues(httpParam.value()), true);
                continue;
            }

            // 如果是单值，则需要进一步进行转换
            final String stringValue = req.getParameter(httpParam.value());
            if (StringUtils.isBlank(stringValue)) {
                continue;
            }

            // int
            if (isAssignableFrom(fieldType, int.class, Integer.class)) {
                try {
                    FieldUtils.writeField(field, command, Integer.valueOf(stringValue), true);
                } catch (NumberFormatException nfe) {
                }
                continue;
            }

            // boolean
            if (isAssignableFrom(fieldType, boolean.class, Boolean.class)) {
                FieldUtils.writeField(field, command, Boolean.valueOf(stringValue), true);
                continue;
            }

            // String
            if (isAssignableFrom(fieldType, String.class)) {
                FieldUtils.writeField(field, command, stringValue, true);
                continue;
            }

        }
    }

    // 向命令注入@Resource依赖
    private void injectResource(final Command command) throws IllegalAccessException {
        for (final Field field : FieldUtils.getFieldsWithAnnotation(command.getClass(), Resource.class)) {

            final Class<?> fieldType = field.getType();

            if (isAssignableFrom(fieldType, ModuleEventWatcher.class)) {
                FieldUtils.writeField(field, command, moduleEventWatcher, true);
                continue;
            }

            if (isAssignableFrom(fieldType, GaReflectSearchManager.class)) {
                FieldUtils.writeField(field, command, new DefaultGaReflectSearchManager(loadedClassDataSource), true);
                continue;
            }

        }
    }

    // 实例化命令对象
    private Command newInstanceCommand(final Class<? extends Command> commandClass,
                                       final HttpServletRequest req) throws IllegalAccessException, InstantiationException {
        final Command command = commandClass.newInstance();
        injectHttpParam(command, req);
        injectResource(command);
        return command;
    }


    /*
     * /greys/COMMAND?QUERY_PARAM_STRING
     */
    @Override
    public WebSocketConnectionListener onAccept(final HttpServletRequest req,
                                                final String protocol) {

        // 映射命令，找不到则拒绝连接
        final Class<? extends Command> commandClass = mappingCommand(req.getPathInfo());
        if (null == commandClass) {
            logger.warn("client={} request path={} failed, command not found.",
                    req.getRemoteAddr(), req.getPathInfo());
            return null;
        }

        try {
            final Command command = newInstanceCommand(commandClass, req);
            logger.debug("client={} was coming, command={};query={};",
                    req.getRemoteAddr(), commandClass.getName(), req.getQueryString());
            return command;
        } catch (Throwable cause) {
            logger.warn("client={} request path={} failed.",
                    req.getRemoteAddr(), req.getPathInfo(), cause);
            return null;
        }

    }

}
