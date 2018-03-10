package com.github.ompc.greys.console;

import com.github.ompc.greys.console.command.GaCommand;
import com.github.ompc.greys.console.command.GaCommand.GaCommands;
import com.github.ompc.greys.console.util.GaConsoleStringUtils;
import jline.console.history.History;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.Flushable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.github.ompc.greys.console.GaConsole.State.WAITING_COMMAND;
import static com.github.ompc.greys.console.GaConsole.State.WAITING_INPUT;
import static com.github.ompc.greys.console.util.GaConsoleStringUtils.splitForLine;
import static java.lang.System.err;
import static java.lang.System.out;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class GaLauncher {

    private static final Logger logger = LoggerFactory.getLogger(GaLauncher.class);

    private final GaConsoleConfig gaConsoleCfg;
    private final GaConsole gaConsole;
    private final ExecutorService commandExecutor;

    public GaLauncher(GaConsoleConfig gaConsoleCfg) throws IOException {
        this.gaConsoleCfg = gaConsoleCfg;
        this.gaConsole = new GaConsole(new GaConsole.InterruptCallback() {
            @Override
            public void interrupt() {

            }
        });
        this.commandExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable run) {
                final Thread gaCmdWorker = new Thread(run, "ga-console-cmd-worker");
                gaCmdWorker.setDaemon(true);
                return gaCmdWorker;
            }
        });
    }

    /**
     * 启动控制台读线程
     */
    private void startupGaConsoleReader() {

        final Thread gaConsoleReader = new Thread("ga-console-reader-daemon") {

            /**
             * 等待从GaConsole完整的输入一行
             *
             * @return 完整的一行命令
             * @throws IOException 从GaConsole读一行发生IO异常
             */
            private String waitingForLine() throws IOException {
                StringBuilder lineBuffer = new StringBuilder();
                while (true) {
                    final String line = gaConsole.getConsoleReader().readLine();
                    // 如果是\结尾，则说明还有下文，需要对换行做特殊处理
                    if (StringUtils.endsWith(line, "\\")) {
                        // 去掉结尾的\
                        lineBuffer.append(line.substring(0, line.length() - 1));
                        continue;
                    } else {
                        lineBuffer.append(line);
                    }
                    break;
                }
                gaConsole.flushHistoryIfNecessary();
                return lineBuffer.toString();
            }


            @Override
            public void run() {

                try {

                    while (!isInterrupted()) {

                        final String line = waitingForLine();

                        // 用户输了一个空行...
                        if (StringUtils.isBlank(line)) {
                            continue;
                        }

                        gaConsole.changeState(WAITING_COMMAND);
                        try {
                            final GaCommand gaCommand = GaCommands.instance.parseGaCommand(splitForLine(line));
                            commandExecutor.submit(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        gaCommand.execute(gaConsole.getConsoleWriter());
                                    } catch (Throwable cause) {
                                        logger.warn("execute command line : {} occur en error!", line, cause);
                                    } finally {
                                        gaConsole.changeState(WAITING_INPUT);
                                    }
                                }
                            });
                        } catch (Throwable cause) {
                            logger.warn("prepare command line : {} occur en error!", line, cause);
                            gaConsole.getConsoleWriter().println(cause.getMessage());
                            gaConsole.changeState(WAITING_INPUT);
                        }

                    }

                } catch (Throwable cause) {
                    logger.warn("ga-console read fail!", cause);
                    err("read fail : %s", cause.getMessage());
                }
            }
        };

        gaConsoleReader.setDaemon(true);
        gaConsoleReader.start();
        logger.info("{} is startup.", gaConsoleReader.getName());

    }

    private static void err(String format, Object... args) {
        err.println(String.format(format, args));
    }

    private static void out(String format, Object... args) {
        out.println(String.format(format, args));
    }


    /**
     * GreysClient主入口
     *
     * @param args $1 : 目标服务器IP地址
     *             $2 : 目标服务器端口号
     *             $3 : 目标jvm-sandbox命名空间
     */
    public static void main(String... args) {

        final GaConsoleConfig gaConsoleCfg = new GaConsoleConfig();
        new CommandLine(gaConsoleCfg)
                .setStopAtUnmatched(true)
                .parse(args);

        assert isNotBlank(gaConsoleCfg.getIp());
        assert isNotBlank(gaConsoleCfg.getNamespace());
        assert gaConsoleCfg.getPort() > 0;

        logger.info("ga-console startup, " +
                        "will communication to \"ws://{}:{}/sandbox/{}/module/websocket/greys/*\" " +
                        "with:{connect-timeout={};timeout={};}.",
                gaConsoleCfg.getIp(),
                gaConsoleCfg.getPort(),
                gaConsoleCfg.getNamespace(),
                gaConsoleCfg.getConnectTimeoutSec(),
                gaConsoleCfg.getTimeoutSec()
        );

    }

}
