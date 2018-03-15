package com.github.ompc.greys.console;

import com.github.ompc.greys.console.command.GaCommands;
import com.github.ompc.greys.console.command.GaCommands.GaCommand;
import com.github.ompc.greys.console.command.GaCommands.GaCommandInitializationException;
import com.github.ompc.greys.console.command.GaCommands.GaCommandNotFoundException;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.ompc.greys.console.GaConsole.State.WAITING_COMMAND;
import static com.github.ompc.greys.console.GaConsole.State.WAITING_INPUT;
import static com.github.ompc.greys.console.util.GaConsoleStringUtils.splitForLine;
import static java.lang.String.format;
import static java.lang.System.err;
import static java.lang.System.out;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class GaLauncher {

    private static final Logger logger = LoggerFactory.getLogger(GaLauncher.class);
    private static final int PING_INTERVAL_SEC = 10;

    private final GaConsoleConfig gaConsoleCfg;
    private final OkHttpClient okHttpClient;
    private final GaConsole gaConsole;

    // 命令执行线程池
    private final ExecutorService commandExecutor;

    // 当前正在进行的命令
    private final AtomicReference<GaCommand> currentGaCommand
            = new AtomicReference<GaCommand>();

    public GaLauncher(final GaConsoleConfig cfg) throws IOException {
        this.gaConsoleCfg = cfg;

        // 初始化HttpClient
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(cfg.getConnectTimeoutSec(), TimeUnit.SECONDS)
                .readTimeout(cfg.getTimeoutSec(), TimeUnit.SECONDS)
                .writeTimeout(cfg.getTimeoutSec(), TimeUnit.SECONDS)
                .pingInterval(PING_INTERVAL_SEC, TimeUnit.SECONDS)
                .build();

        // 初始化命令执行器
        this.commandExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable run) {
                final Thread gaCmdWorker = new Thread(run, "ga-console-cmd-worker");
                gaCmdWorker.setDaemon(true);
                return gaCmdWorker;
            }
        });

        // 初始化Greys控制台
        this.gaConsole = new GaConsole(new GaConsole.InterruptCallback() {
            @Override
            public void interrupt() {
                // spin to got current GaCommand
                while (true) {
                    final GaCommand oriGaCommand = currentGaCommand.get();
                    if (switchCurrentGaCommand(oriGaCommand, null)) {
                        terminateCurrentGaCommand(oriGaCommand);
                        break;
                    }
                }
            }
        });

        loopForRead();
    }


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

    private void loopForRead() {

        try {
            while (true) {

                final String line = waitingForLine();

                // 用户输了一个空行...
                if (StringUtils.isBlank(line)) {
                    logger.debug("ignore empty line.");
                    continue;
                }
                logger.debug("input {};", line);

                try {
                    final GaCommand gaCommand = GaCommands.instance.parseGaCommand(splitForLine(line));
                    logger.debug("parse cmd: {} -> {};", line, gaCommand);
                    if (!switchCurrentGaCommand(null, gaCommand)) {
                        logger.warn("there is now an ongoing command:{} that is not over, ignore this time.", currentGaCommand.get());
                        continue;
                    }
                    logger.debug("switched current command to {}", gaCommand);

                    // 异步命令执行，不要阻塞输入线程，因为还有"CTRL_D"要响应
                    commandExecutor.submit(new Runnable() {
                        @Override
                        public void run() {

                            // 开始执行命令
                            try {
                                logger.debug("{} execute Before.", gaCommand);
                                gaCommand.execute(okHttpClient, gaConsoleCfg, gaConsole.getConsoleWriter());
                                logger.debug("{} execute Return.", gaCommand);
                            }

                            // 命令执行失败
                            catch (Throwable cause) {
                                logger.warn("{} execute failed!", gaCommand, cause);
                                consoleErr("execute failed: %s", cause.getMessage());
                            }

                            // 命令执行完毕，无论结果如何，都需要完成对应的操作
                            // 1. 切换当前命令
                            // 2. 刷新输入行
                            finally {
                                if (switchCurrentGaCommand(gaCommand, null)) {
                                    terminateCurrentGaCommand(gaCommand);
                                } else {
                                    // 如果切换失败，可能是因为之前的命令已经取消，当前命令已经被其他命令占用了
                                    logger.info("another command is ongoing, maybe {} was cancel.", gaCommand);
                                }
                            }
                        }
                    });
                } catch (Throwable cause) {
                    logger.warn("prepare line occur en error! \"{}\"", line, cause);
                    // 命令不存在
                    if (cause instanceof GaCommandNotFoundException) {
                        consoleErr("\"%s\" not found.", ((GaCommandNotFoundException) cause).getName());
                    }

                    // 命令初始化失败
                    else if (cause instanceof GaCommandInitializationException) {
                        consoleErr("\"%s\" init failed.", ((GaCommandInitializationException) cause).getName());
                    }

                    // 其他未知错误
                    else {
                        consoleErr("execute failed : %s", cause.getMessage());
                    }
                }

            }

        } catch (Throwable cause) {
            logger.warn("ga-console read fail!", cause);
            cause.printStackTrace(System.err);
        }

    }


    /*
     * 结束当前命令
     */
    private void terminateCurrentGaCommand(GaCommand gaCommand) {
        if (null == gaCommand) {
            return;
        }
        gaCommand.terminate();
        logger.debug("{} was terminated.", gaCommand);
    }

    /**
     * 切换当前命令
     */
    private boolean switchCurrentGaCommand(final GaCommand expected,
                                           final GaCommand current) {
        final boolean isSwitched;
        if (isSwitched = currentGaCommand.compareAndSet(expected, current)) {
            gaConsole.changeState(
                    null == current
                            ? WAITING_INPUT
                            : WAITING_COMMAND
            );
        }
        return isSwitched;
    }

    private void consoleOut(String format, Object... args) {
        gaConsole.getConsoleWriter().println(format(format, args));
    }

    private void consoleErr(String format, Object... args) {
        consoleOut("ERROR: " + format, args);
    }

    private static void err(String format, Object... args) {
        err.println(format(format, args));
    }

    private static void out(String format, Object... args) {
        out.println(format(format, args));
    }


    /**
     * GreysClient主入口
     *
     * @param args $1 : 目标服务器IP地址
     *             $2 : 目标服务器端口号
     *             $3 : 目标jvm-sandbox命名空间
     */
    public static void main(String... args) throws IOException {

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
        new GaLauncher(gaConsoleCfg);

    }

}
