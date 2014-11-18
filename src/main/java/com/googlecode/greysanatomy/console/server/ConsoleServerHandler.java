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
import com.googlecode.greysanatomy.probe.ProbeJobs;
import com.googlecode.greysanatomy.util.JvmUtils;
import com.googlecode.greysanatomy.util.JvmUtils.ShutdownHook;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

<<<<<<< HEAD
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.instrument.Instrumentation;
=======
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.instrument.Instrumentation;
import java.nio.CharBuffer;
>>>>>>> pr/8
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

<<<<<<< HEAD
    public RespResult postCmd(final ReqCmd cmd) {
=======
    public RespResult postCmd(final ReqCmd cmd) throws IOException {
>>>>>>> pr/8
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
<<<<<<< HEAD
                        write(respResult.getSessionId(), respResult.getJobId(), isF, message);
=======
                        write(respResult.getJobId(), isF, message);
>>>>>>> pr/8
                    }
                };

                try {
                    final Command command = Commands.getInstance().newRiscCommand(cmd.getCommand());
                    // 命令不存在
                    if (null == command) {
<<<<<<< HEAD
                        write(respResult.getSessionId(), respResult.getJobId(), true, "command not found!");
=======
                        write(respResult.getJobId(), true, "command not found!");
>>>>>>> pr/8
                        return;
                    }
                    final Action action = command.getAction();
                    action.action(consoleServer, info, sender);
                } catch (Throwable t) {
                    // 执行命令失败
                    logger.warn("do action failed.", t);
<<<<<<< HEAD
                    write(respResult.getSessionId(), respResult.getJobId(), true, "do action failed. cause:" + t.getMessage());
=======
                    write(respResult.getJobId(), true, "do action failed. cause : " + t.getMessage());
>>>>>>> pr/8
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
//        logger.info("debug for req={},respResult.message={}",req,respResult.getMessage());
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

<<<<<<< HEAD
    private final String REST_DIR = System.getProperty("java.io.tmpdir")//执行结果输出文件路径
            + File.separator + "greysdata" + File.separator;
    private final String REST_FILE_EXT = ".ga";                            //存储中间结果的临时文件后缀名
=======
>>>>>>> pr/8
    private final String END_MASK = "" + (char) 29;                        //用于标记文件结束的标识符

    /**
     * 写结果
     *
<<<<<<< HEAD
     * @param gaSessionId
=======
>>>>>>> pr/8
     * @param jobId
     * @param isF
     * @param message
     */
<<<<<<< HEAD
    private void write(long gaSessionId, String jobId, boolean isF, String message) {
=======
    private void write(int jobId, boolean isF, String message) {
>>>>>>> pr/8
        //TODO 这里用队列来做缓存，改善写文件性能，否则可能会影响被probe代码的效率
        if (isF) {
            message += END_MASK;
        }

        if (StringUtils.isEmpty(message)) {
            return;
        }

<<<<<<< HEAD
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
=======
        final Writer writer = ProbeJobs.getJobWriter(jobId);
        if (null != writer) {
            try {
                writer.append(message);
                writer.flush();
            } catch (IOException e) {
                logger.warn("write job message failed, jobId={}.", jobId, e);
            }
        }


>>>>>>> pr/8
    }

    /**
     * 读job的结果
     *
     * @param jobId
     * @param pos
     * @param respResult
     */
<<<<<<< HEAD
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
        } catch (FileNotFoundException fnfe) {
            logger.info("jobId={} was not ready yet.",jobId);
        } catch (IOException e) {
            logger.warn("jobId={}'s file read error!", jobId, e);
            return;
        } finally {
            if (null != rf) {
                try {
                    rf.close();
                } catch (Exception e) {
                    //
                }
=======
    private void read(int jobId, int pos, RespResult respResult) {

        final CharBuffer buffer = CharBuffer.allocate(4028);
        final Reader reader = ProbeJobs.getJobReader(jobId);
        if (null != reader) {
            try {
                final int newPos = pos + reader.read(buffer);
                buffer.flip();
                respResult.setPos(newPos);
                respResult.setMessage(buffer.toString());
            } catch (IOException e) {
                logger.warn("read job failed, jobId={}.", jobId, e);
>>>>>>> pr/8
            }
        }

    }

<<<<<<< HEAD
    private String getExecuteFilePath(String jobId) {
        return REST_DIR + jobId + REST_FILE_EXT;
    }

=======
>>>>>>> pr/8
    private boolean isFinish(String message) {
        return !StringUtils.isEmpty(message) ? message.endsWith(END_MASK) : false;
    }
}
