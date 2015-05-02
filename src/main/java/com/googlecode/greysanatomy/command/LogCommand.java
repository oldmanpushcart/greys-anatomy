package com.googlecode.greysanatomy.command;

import com.googlecode.greysanatomy.command.annotation.Cmd;
import com.googlecode.greysanatomy.command.annotation.IndexArg;
import com.googlecode.greysanatomy.server.GaServer;
import com.googlecode.greysanatomy.util.GaStringUtils;
import com.googlecode.greysanatomy.util.LogUtils;
import com.googlecode.greysanatomy.util.LogUtils.LogLevel;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * 日志级别调整命令
 * Created by vlinux on 15/3/8.
 */
@Cmd(named = "log", sort = 7, desc = "Change the log level.",
        eg = {
                "log",
                "log debug",
                "log info",
                "log warn",
                "log error",
                "log DeBuG"
        })
public class LogCommand extends Command {

    private final Map<String, LogLevel> logLevelMapping = new HashMap<String, LogLevel>();

    public LogCommand() {
        logLevelMapping.put("DEBUG", LogLevel.DEBUG);
        logLevelMapping.put("INFO", LogLevel.INFO);
        logLevelMapping.put("WARN", LogLevel.WARN);
        logLevelMapping.put("ERROR", LogLevel.ERROR);
    }

    @IndexArg(index = 0, name = "log-level", description = "the log level, debug/info/warn/error.", isRequired = false)
    private String level;

    private boolean isShowLevel() {
        return GaStringUtils.isBlank(level) || !logLevelMapping.containsKey(level.toUpperCase());
    }

    @Override
    public Action getAction() {
        return new Action() {

            @Override
            public void action(final GaServer gaServer, final Info info, final Sender sender) throws Throwable {

                final StringBuilder message = new StringBuilder();
                final LogLevel beforeLogLevel = LogUtils.currentLogLevel();

                // show log-level
                if (isShowLevel()) {
                    message.append(format("current log-level : %s", beforeLogLevel));
                }

                // change log-level
                else {
                    final LogLevel newLogLevel = logLevelMapping.get(level.toUpperCase());
                    LogUtils.changeLogLevel(newLogLevel);
                    message.append(format("change log-level : (%s->%s).", beforeLogLevel, newLogLevel));
                }

                sender.send(true, message.toString());
            }

        };
    }
}
