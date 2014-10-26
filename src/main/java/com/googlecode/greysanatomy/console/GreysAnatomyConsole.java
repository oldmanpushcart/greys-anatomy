package com.googlecode.greysanatomy.console;

import com.googlecode.greysanatomy.Configer;
import com.googlecode.greysanatomy.console.command.Command;
import com.googlecode.greysanatomy.console.command.Commands;
import com.googlecode.greysanatomy.console.command.ShutdownCommand;
import com.googlecode.greysanatomy.console.rmi.RespResult;
import com.googlecode.greysanatomy.console.rmi.req.ReqCmd;
import com.googlecode.greysanatomy.console.rmi.req.ReqGetResult;
import com.googlecode.greysanatomy.console.rmi.req.ReqKillJob;
import com.googlecode.greysanatomy.console.server.ConsoleServerService;
import com.googlecode.greysanatomy.exception.ConsoleException;
import com.googlecode.greysanatomy.util.GaStringUtils;
import jline.console.ConsoleReader;
import jline.console.KeyMap;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.rmi.NoSuchObjectException;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * 控制台
 *
 * @author vlinux
 */
public class GreysAnatomyConsole {

    private static final Logger logger = LoggerFactory.getLogger("greysanatomy");

    private final Configer configer;
    private final ConsoleReader console;

    private volatile boolean isF = true;
    private volatile boolean isShutdown = false;

    private final long sessionId;
    private String jobId;
    private String path;

    /**
     * 创建GA控制台
     *
     * @param configer
     * @throws IOException
     */
    public GreysAnatomyConsole(Configer configer, long sessionId) throws IOException {
        this.console = new ConsoleReader(System.in, System.out);
        this.configer = configer;
        this.sessionId = sessionId;
        write(GaStringUtils.getLogo());
        Commands.getInstance().registCompleter(console);
    }

    /**
     * 控制台输入者
     *
     * @author vlinux
     */
    private class GaConsoleInputer implements Runnable {

        private final ConsoleServerService consoleServer;

        private GaConsoleInputer(ConsoleServerService consoleServer) {
            this.consoleServer = consoleServer;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    //控制台读命令
                    doRead();
                } catch (ConsoleException ce) {
                    write("Error : "+ce.getMessage()+"\n");
                    write("Please type help for more information...\n\n");
                } catch (Exception e) {
                    // 这里是控制台，可能么？
                    logger.warn("console read failed.", e);
                }
            }
        }

        private void doRead() throws Exception {
            final String prompt = isF ? configer.getConsolePrompt() : EMPTY;
            final ReqCmd reqCmd = new ReqCmd(console.readLine(prompt), sessionId);

			/*
             * 如果读入的是空白字符串或者当前控制台没被标记为已完成
			 * 则放弃本次所读取内容
			 */
            if (isBlank(reqCmd.getCommand()) || !isF) {
                return;
            }

            final Command command;
            try {
                command = Commands.getInstance().newRiscCommand(reqCmd.getCommand());
            } catch (Exception e) {
                throw new ConsoleException(e.getMessage());
            }


            if (command != null) {
                path = command.getRedirectPath();
                if (!StringUtils.isEmpty(path)) {
                    //发命令之前先把重定向文件创建好，如果没有权限或其他问题，就不发起任务
                    try {
                        new File(path).createNewFile();
                    } catch (Exception e) {
                        final String msg = String.format("create path:%s failed. %s", path, e.getMessage());
                        logger.warn(msg, e);
                        write(msg);
                        return;
                    }
                }
            } else {
                //如果命令不存在，客户端不抛异常，交给服务端处理。但是需要把path清空
                path = EMPTY;
            }

            // 将命令状态标记为未完成
            isF = false;

            // 用户执行了一个shutdown命令,终端需要退出
            if (command instanceof ShutdownCommand) {
                isShutdown = true;
            }

            // 发送命令请求
            RespResult result = consoleServer.postCmd(reqCmd);
            jobId = result.getJobId();
        }

    }

    /**
     * 控制台输出者
     *
     * @author chengtongda
     */
    private class GaConsoleOutputer implements Runnable {

        private final ConsoleServerService consoleServer;
        private String currentJob;
        private int pos = 0;

        private GaConsoleOutputer(ConsoleServerService consoleServer) {
            this.consoleServer = consoleServer;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    //控制台写数据
                    doWrite();
                    //每500ms读一次结果
                    Thread.sleep(500);
                } catch (NoSuchObjectException nsoe) {
                    // 目标RMI关闭,需要退出控制台
                    logger.warn("target RMI's server was closed, console will be exit.");
                    break;
                } catch (Exception e) {
                    logger.warn("console write failed.", e);
                }
            }
        }

        private void doWrite() throws Exception {
            //如果任务结束，或还没有注册好job  则不读
            if (isF || sessionId == 0 || StringUtils.isEmpty(jobId)) {
                return;
            }

            //如果当前获取结果的job不是正在执行的job，则从0开始读
            if (!StringUtils.equals(currentJob, jobId)) {
                pos = 0;
                currentJob = jobId;
            }

            RespResult resp = consoleServer.getCmdExecuteResult(new ReqGetResult(jobId, sessionId, pos));
            pos = resp.getPos();

            //先写重定向
            try {
                writeToFile(resp.getMessage(), path);
            } catch (IOException e) {
                //重定向写文件出现异常时，需要kill掉job 不执行了
                consoleServer.killJob(new ReqKillJob(sessionId, jobId));
                isF = true;
                logger.warn("writeToFile failed.", e);
                write(path + ":" + e.getMessage());
                return;
            }

            write(resp);

            if (isShutdown) {
                logger.info("greys console will be shutdown.");
                System.exit(0);
            }

        }

    }

    /**
     * 启动console
     *
     * @param consoleServer
     */
    public synchronized void start(final ConsoleServerService consoleServer) {
        this.console.getKeys().bind("" + KeyMap.CTRL_D, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isF) {
                    write("abort it.");
                    isF = true;
                    try {
                        consoleServer.killJob(new ReqKillJob(sessionId, jobId));
                    } catch (Exception e1) {
                        // 这里是控制台，可能么？
                        logger.warn("killJob failed.", e);
                    }
                }
            }

        });
        new Thread(new GaConsoleInputer(consoleServer), "ga-console-inputer").start();
        new Thread(new GaConsoleOutputer(consoleServer), "ga-console-outputer").start();
    }


    /**
     * 向控制台输出返回信息
     *
     * @param resp
     */
    private void write(RespResult resp) {
        if (!isF) {
            String content = resp.getMessage();
            if (resp.isFinish()) {
                isF = true;
                //content += "\n------------------------------end------------------------------\n";
                content += "\n";
            }
            if (!StringUtils.isEmpty(content)) {
                write(content);
            }
        }
    }

    /**
     * 输出信息
     *
     * @param message
     */
    private void write(String message) {
        final Writer writer = console.getOutput();
        try {
            writer.write(message);
            writer.flush();
        } catch (IOException e) {
            // 控制台写失败，可能么？
            logger.warn("console write failed.", e);
        }

    }

    /**
     * 输出信息到文件
     *
     * @param message
     * @param path
     * @throws IOException
     */
    private void writeToFile(String message, String path) throws IOException {
        if (StringUtils.isEmpty(message) || StringUtils.isEmpty(path)) {
            return;
        }

        PrintWriter out = null;
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(path, true)));
            out.println(message);
            out.flush();
        } finally {
            if (null != out) {
                out.close();
            }
        }
    }
}
