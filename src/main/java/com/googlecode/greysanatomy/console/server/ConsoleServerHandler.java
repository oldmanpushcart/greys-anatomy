package com.googlecode.greysanatomy.console.server;

import com.googlecode.greysanatomy.console.command.Command;
import com.googlecode.greysanatomy.console.command.Command.Action;
import com.googlecode.greysanatomy.console.command.Command.Info;
import com.googlecode.greysanatomy.console.command.Command.Sender;
import com.googlecode.greysanatomy.console.command.Commands;
import com.googlecode.greysanatomy.console.rmi.RespResult;
import com.googlecode.greysanatomy.console.rmi.req.ReqCmd;
import com.googlecode.greysanatomy.console.rmi.req.ReqGetResult;
import com.googlecode.greysanatomy.console.rmi.req.ReqHeart;
import com.googlecode.greysanatomy.console.rmi.req.ReqKillJob;
import com.googlecode.greysanatomy.util.JvmUtils;
import com.googlecode.greysanatomy.util.JvmUtils.ShutdownHook;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.googlecode.greysanatomy.console.server.SessionJobsHolder.*;
import static com.googlecode.greysanatomy.probe.ProbeJobs.createJob;

/**
 * 控制台服务端处理器
 *
 * @author vlinux
 */
public class ConsoleServerHandler {

    private static final Logger logger = LoggerFactory.getLogger("greysanatomy");

    private final ConsoleServer consoleServer;
    private final Instrumentation inst;
    private final ExecutorService workers;

    public ConsoleServerHandler(ConsoleServer consoleServer, Instrumentation inst) {
        this.consoleServer = consoleServer;
        this.inst = inst;
        this.workers = Executors.newCachedThreadPool(new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "ga-console-server-workers");
                t.setDaemon(true);
                return t;
            }

        });

        JvmUtils.registShutdownHook("ga-console-server", new ShutdownHook() {

            @Override
            public void shutdown() throws Throwable {
                if (null != workers) {
                    workers.shutdown();
                }
            }

        });

    }

    public RespResult postCmd(final ReqCmd cmd) {
        final RespResult respResult = new RespResult();
        respResult.setSessionId(cmd.getGaSessionId());
        respResult.setJobId(createJob());
        workers.execute(new Runnable() {

            @Override
            public void run() {
                // 普通命令请求
                final Info info = new Info(inst, respResult.getSessionId(), respResult.getJobId());
                final Sender sender = new Sender() {

                    @Override
                    public void send(boolean isF, String message) {
                        write(respResult.getSessionId(), respResult.getJobId(), isF, message);
                    }
                };

                try {
                    final Command command = Commands.getInstance().newCommand(cmd.getCommand());
                    // 命令不存在
                    if (null == command) {
                        write(respResult.getSessionId(), respResult.getJobId(), true, "command not found!");
                        return;
                    }
                    final Action action = command.getAction();
                    action.action(consoleServer, info, sender);
                } catch (Throwable t) {
                    // 执行命令失败
                    logger.warn("do action failed.", t);
                    write(respResult.getSessionId(), respResult.getJobId(), true, "do action failed. cause:" + t.getMessage());
                    return;
                }
            }

        });
        return respResult;
    }

    public long register() {
        return registSession();
    }

    public RespResult getCmdExecuteResult(ReqGetResult req) {
        RespResult respResult = new RespResult();
        if (!heartBeatSession(req.getGaSessionId())) {
            respResult.setMessage("session Timeout.please reload!");
            respResult.setFinish(true);
            return respResult;
        }
        read(req.getJobId(), req.getPos(), respResult);
        respResult.setFinish(isFinish(respResult.getMessage()));
        return respResult;
    }

    /**
     * 干掉一个Job
     *
     * @param req
     */
    public void killJob(ReqKillJob req) {
        unRegistJob(req.getGaSessionId(), req.getJobId());
    }

    /**
     * 会话心跳
     *
     * @param req
     * @return
     */
    public boolean sessionHeartBeat(ReqHeart req) {
        return heartBeatSession(req.getGaSessionId());
    }

    private final String REST_DIR = System.getProperty("java.io.tmpdir")//执行结果输出文件路径
            + File.separator + "greysdata" + File.separator;
    private final String REST_FILE_EXT = ".ga";                            //存储中间结果的临时文件后缀名
    private final String END_MASK = "" + (char) 29;                        //用于标记文件结束的标识符

    /**
     * 写结果
     *
     * @param gaSessionId
     * @param jobId
     * @param isF
     * @param message
     */
    private void write(long gaSessionId, String jobId, boolean isF, String message) {
        //TODO 这里用队列来做缓存，改善写文件性能，否则可能会影响被probe代码的效率
        if (isF) {
            message += END_MASK;
        }

        if (StringUtils.isEmpty(message)) {
            return;
        }

        RandomAccessFile rf = null;

        try {
            new File(REST_DIR).mkdir();
            rf = new RandomAccessFile(getExecuteFilePath(jobId), "rw");
            rf.seek(rf.length());
            rf.write(message.getBytes());
        } catch (IOException e) {
            logger.warn("jobFile write error!", e);
            return;
        } finally {
            if (null != rf) {
                try {
                    rf.close();
                } catch (Exception e) {
                    //
                }
            }
        }
    }

    /**
     * 读job的结果
     * @param jobId
     * @param pos
     * @param respResult
     */
    private void read(String jobId, int pos, RespResult respResult) {
        int newPos = pos;
        final StringBuilder sb = new StringBuilder();
        RandomAccessFile rf = null;
        try {
            rf = new RandomAccessFile(getExecuteFilePath(jobId), "r");
            rf.seek(pos);
            byte[] buffer = new byte[10000];
            int len = 0;
            while ((len = rf.read(buffer)) != -1) {
                newPos += len;
                sb.append(new String(buffer, 0, len));
            }
            respResult.setPos(newPos);
            respResult.setMessage(sb.toString());
        } catch (IOException e) {
            logger.warn("jobFile read error!");
            return;
        } finally {
            if (null != rf) {
                try {
                    rf.close();
                } catch (Exception e) {
                    //
                }
            }
        }

    }

    private String getExecuteFilePath(String jobId) {
        return REST_DIR + jobId + REST_FILE_EXT;
    }

    private boolean isFinish(String message) {
        return !StringUtils.isEmpty(message) ? message.endsWith(END_MASK) : false;
    }
}
