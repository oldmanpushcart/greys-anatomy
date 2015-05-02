package com.googlecode.greysanatomy.server;

import com.googlecode.greysanatomy.command.Command;
import com.googlecode.greysanatomy.command.Commands;
import com.googlecode.greysanatomy.command.QuitCommand;
import com.googlecode.greysanatomy.command.ShutdownCommand;
import com.googlecode.greysanatomy.exception.CommandInitializationException;
import com.googlecode.greysanatomy.exception.CommandNotFoundException;
import com.googlecode.greysanatomy.probe.ProbeJobs;
import com.googlecode.greysanatomy.util.GaStringUtils;
import com.googlecode.greysanatomy.util.LogUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.instrument.Instrumentation;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * 命令处理器
 * Created by vlinux on 15/5/2.
 */
public class CommandHandler {


    private final Logger logger = LogUtils.getLogger();

    private static final int BUFFER_SIZE = 4 * 1024;

    private final GaServer gaServer;
    private final Instrumentation instrumentation;
    private final ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            final Thread t = new Thread(r, "GaCommand-execute-daemon");
            t.setDaemon(true);
            return t;
        }
    });

    public CommandHandler(GaServer gaServer, Instrumentation instrumentation) {
        this.gaServer = gaServer;
        this.instrumentation = instrumentation;
    }

    /**
     * 解析输入行并执行命令
     *
     * @param line      输入的命令行
     * @param gaSession 会话
     * @throws IOException IO错误
     */
    public void executeCommand(final String line, final GaSession gaSession) throws IOException {

        final SocketChannel socketChannel = gaSession.getSocketChannel();

        try {

            final Command command = Commands.getInstance().newCommand(line);

            // 无论什么命令，先执行本体
            execute(gaSession, socketChannel, command);

        }

        // 命令不存在
        catch (CommandNotFoundException e) {
            final String message = format("command \"%s\" not found.\n", e.getCommand());
            write(socketChannel, message, gaSession.getCharset());
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, message, e);
            }
        }

        // 命令初始化失败
        catch (CommandInitializationException e) {
            final String message = format("command \"%s\"init failed.\n", e.getCommand());
            write(socketChannel, message, gaSession.getCharset());
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, message, e);
            }
        }

        // 命令内部错误
        catch (Throwable t) {
            final String message = format("command execute failed : %s\n", GaStringUtils.getCauseMessage(t));
            write(socketChannel, message, gaSession.getCharset());
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, message);
            }

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, message, t);
            }
        }
    }

    private void execute(final GaSession gaSession, final SocketChannel socketChannel, final Command command) throws IOException {
        final AtomicBoolean isFinishRef = new AtomicBoolean(false);
        final int jobId = ProbeJobs.createJob();
        gaSession.setCurrentJobId(jobId);

        final Command.Action action = command.getAction();
        final Command.Info info = new Command.Info(instrumentation, gaSession.getSessionId(), jobId);
        final Command.Sender sender = new Command.Sender() {

            @Override
            public void send(boolean isF, String message) {

                final Writer writer = ProbeJobs.getJobWriter(jobId);
                if (null != writer) {
                    try {
                        writer.write(message);
                        writer.flush();
                    } catch (IOException e) {
                        if (logger.isLoggable(Level.WARNING)) {
                            logger.log(Level.WARNING, format("GaSession[%d].JobId[%d] write failed.",
                                    gaSession.getSessionId(),
                                    gaSession.getCurrentJobId()), e);
                        }
                        isFinishRef.set(true);
                    }

                }

                if (isF) {
                    isFinishRef.set(true);
                }

            }

        };

        executorService.execute(new Runnable() {
            @Override
            public void run() {

                final CharBuffer buffer = CharBuffer.allocate(BUFFER_SIZE);

                // 先将会话的写打开
                gaSession.markWritable(true);

                try {

                    final Thread currentThread = Thread.currentThread();
                    action.action(gaServer, info, sender);

                    while (!gaSession.isDestroy()
                            && gaSession.isWritable()
//                                && !isFinishRef.get()
                            && !currentThread.isInterrupted()) {

                        final Reader reader = ProbeJobs.getJobReader(jobId);
                        if (null != reader) {

                            // touch the session
                            gaSession.touch();

                            // 首先将一部分数据读取到buffer中
                            if (-1 == reader.read(buffer)) {
                                // arrive job EOF
                                if (logger.isLoggable(Level.FINE)) {
                                    logger.log(Level.FINE, format("GaSession[%d].JobId[%d] arrive EOF.",
                                            gaSession.getCurrentJobId(),
                                            gaSession.getSessionId()));
                                }

                                // 当读到EOF的时候，同时Sender标记为isFinished
                                // 说明整个命令结束了，标记整个会话为不可写，结束命令循环
                                if (isFinishRef.get()) {
                                    gaSession.markWritable(false);
                                }

                                // 若已经临时达到EOF，说明需要休息下
                                // 间隔500ms，人类操作无感知
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    currentThread.interrupt();
                                }

                            }

                            // 读出了点东西
                            else {
                                buffer.flip();
                                final char[] charArray = new char[buffer.limit()];
                                buffer.get(charArray);
                                buffer.clear();

                                final ByteBuffer byteBuffer = ByteBuffer.wrap(new String(charArray).getBytes(gaSession.getCharset()));

                                while (byteBuffer.hasRemaining()) {

                                    if (-1 == gaSession.getSocketChannel().write(byteBuffer)) {
                                        // socket broken
                                        if (logger.isLoggable(Level.INFO)) {
                                            logger.log(Level.INFO, format("JobId[%d] write failed, because socket broken, GaSession[%d] will be destroy.",
                                                    jobId,
                                                    gaSession.getSessionId()));
                                            gaSession.destroy();
                                        }
                                    }

                                }//while for write
                            }


                        }

                    }

                }

                // 遇到关闭的链接可以忽略
                catch (ClosedChannelException e) {

                    final String message = format("GaSession[%d].JobId[%d] write failed, because socket broken.\n",
                            gaSession.getSessionId(),
                            jobId);
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, message, e);
                    }

                }

                // 其他错误必须纪录
                catch (Throwable t) {
                    final String message = format("command execute failed, %s\n",
                            GaStringUtils.getCauseMessage(t));
                    if (logger.isLoggable(Level.WARNING)) {
                        logger.log(Level.WARNING, message, t);
                    }

                    try {
                        write(socketChannel, message, gaSession.getCharset());
                    } catch (IOException e) {
                        if (logger.isLoggable(Level.WARNING)) {
                            logger.log(Level.WARNING, format("%s write failed, will be destroy.", gaSession), e);
                        }
                        gaSession.destroy();
                    }
                } finally {

                    // 无论命令的结局如何，必须要关闭掉会话的写
                    gaSession.markWritable(false);

                    // 杀死后台JOB
                    final Integer jobId = gaSession.getCurrentJobId();
                    if (null != jobId) {
                        ProbeJobs.killJob(jobId);
                    }


                    // 推出命令，需要关闭Socket
                    if (command instanceof QuitCommand) {
                        gaSession.destroy();
                    }

                    // 关闭命令，需要关闭整个服务端
                    else if (command instanceof ShutdownCommand) {
                        CommandHandler.this.gaServer.unbind();
                    }

                }

            }
        });
    }

    private void write(SocketChannel socketChannel, String message, Charset charset) throws IOException {
        socketChannel.write(ByteBuffer.wrap(message.getBytes(charset)));
    }

    public void destroy() {
        executorService.shutdown();
    }


}
