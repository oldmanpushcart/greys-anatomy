package com.googlecode.greysanatomy.util;

import java.util.logging.*;

import static java.lang.String.format;
import static java.util.logging.Level.*;

/**
 * 日志工具类
 * Created by vlinux on 15/3/8.
 */
public class LogUtils {

    private static final Logger logger = Logger.getLogger("greys-anatomy");

    static {
        logger.addHandler(new ConsoleHandler());
        setLevel(INFO);
    }

    public static enum LogLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    private static void setLevel(final Level level) {
        logger.setLevel(level);
        final Handler[] handlers = logger.getHandlers();
        if( null != handlers ) {
            for( Handler handler : handlers ) {
                handler.setLevel(level);
            }
        }
    }

    public static void changeLogLevel(final LogLevel logLevel) {
        switch (logLevel) {
            case DEBUG: {
                setLevel(FINE);
                break;
            }

            default:
            case INFO: {
                setLevel(INFO);
                break;
            }

            case WARN: {
                setLevel(WARNING);
                break;
            }

            case ERROR: {
                setLevel(SEVERE);
                break;
            }
        }
    }

    public static LogLevel currentLogLevel() {
        final Level level = logger.getLevel();

        if (level.intValue() <= FINE.intValue()) {
            return LogLevel.DEBUG;
        } else if (level.intValue() <= INFO.intValue()) {
            return LogLevel.INFO;
        } else if (level.intValue() <= WARNING.intValue()) {
            return LogLevel.WARN;
        } else if (level.intValue() <= SEVERE.intValue()) {
            return LogLevel.ERROR;
        }

        // OFF的情况返回INFO
        else {
            return LogLevel.INFO;
        }
    }

    public static void info(String format, Object... args) {
        if (logger.isLoggable(INFO)) {
            logger.log(INFO, format(format, args));
        }
    }

    public static void trace(String format, Object... args) {
        if (logger.isLoggable(FINEST)) {
            logger.log(FINEST, format(format, args));
        }
    }

    public static void debug(String format, Object... args) {
        if (logger.isLoggable(FINE)) {
            logger.log(FINE, format(format, args));
        }
    }

    public static void debug(Throwable t, String format, Object... args) {
        if (logger.isLoggable(FINE)) {
            logger.log(FINE, format(format, args), t);
        }
    }

    public static void warn(String format, Object... args) {
        if (logger.isLoggable(WARNING)) {
            logger.log(WARNING, format(format, args));
        }
    }

    public static void warn(Throwable t, String format, Object... args) {
        if (logger.isLoggable(WARNING)) {
            logger.log(WARNING, format(format, args), t);
        }
    }

    public static void error(Throwable t, String format, Object... args) {
        if (logger.isLoggable(SEVERE)) {
            logger.log(SEVERE, format(format, args), t);
        }
    }

}
