package com.github.ompc.greys.core.util;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Greys日志
 * Created by vlinux on 15/3/8.
 */
public class LogUtil {

    private static final Logger logger;

    static {

        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        final JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(loggerContext);
        loggerContext.reset();
        try {

            configurator.doConfigure(LogUtil.class.getResourceAsStream("/com/github/ompc/greys/core/res/greys-logback.xml"));
        } catch (JoranException e) {
            throw new RuntimeException("load logback config failed, you need restart greys", e);
        } finally {
            logger = LoggerFactory.getLogger("greys-anatomy");
        }

    }

    public static Logger getLogger() {
        return logger;
    }

}
