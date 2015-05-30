package com.github.ompc.greys.server;

import com.github.ompc.greys.Configure;
import com.github.ompc.greys.util.IOUtil;
import com.github.ompc.greys.util.LogUtil;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static com.github.ompc.greys.server.LineDecodeState.READ_CHAR;
import static com.github.ompc.greys.server.LineDecodeState.READ_EOL;
import static com.github.ompc.greys.util.StringUtil.getLogo;
import static java.lang.String.format;
import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;

/**
 * GaServer操作的附件
 * Created by vlinux on 15/5/3.
 */
class GaAttachment {

    private final int bufferSize;
    private final Session session;

    private LineDecodeState lineDecodeState;
    private ByteBuffer lineByteBuffer;


    public GaAttachment(int bufferSize, Session session) {
        this.lineByteBuffer = ByteBuffer.allocate(bufferSize);
        this.bufferSize = bufferSize;
        this.lineDecodeState = READ_CHAR;
        this.session = session;
    }

    public LineDecodeState getLineDecodeState() {
        return lineDecodeState;
    }


    public void setLineDecodeState(LineDecodeState lineDecodeState) {
        this.lineDecodeState = lineDecodeState;
    }

    public void put(byte data) {
        if (lineByteBuffer.hasRemaining()) {
            lineByteBuffer.put(data);
        } else {
            final ByteBuffer newLineByteBuffer = ByteBuffer.allocate(lineByteBuffer.capacity() + bufferSize);
            lineByteBuffer.flip();
            newLineByteBuffer.put(lineByteBuffer);
            newLineByteBuffer.put(data);
            this.lineByteBuffer = newLineByteBuffer;
        }
    }

    public String clearAndGetLine(Charset charset) {
        lineByteBuffer.flip();
        final byte[] dataArray = new byte[lineByteBuffer.limit()];
        lineByteBuffer.get(dataArray);
        final String line = new String(dataArray, charset);
        lineByteBuffer.clear();
        return line;
    }

    public Session getSession() {
        return session;
    }

}

/**
 * 行解码
 */
enum LineDecodeState {

    // 读字符
    READ_CHAR,

    // 读换行
    READ_EOL
}

/**
 * Greys 服务端<br/>
 * Created by vlinux on 15/5/2.
 */
public class GaServer {

    private final Logger logger = LogUtil.getLogger();

    private static final int BUFFER_SIZE = 4 * 1024;
    private static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    private static final byte CTRL_D = 0x04;
    private static final byte CTRL_X = 0x18;
    private static final byte EOT = 0x04;

    private final AtomicBoolean isBindRef = new AtomicBoolean(false);
    private final SessionManager sessionManager;
    private final CommandHandler commandHandler;
    private final int javaPid;

    private final ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            final Thread t = new Thread(r, "ga-command-execute-daemon");
            t.setDaemon(true);
            return t;
        }
    });

    private GaServer(int javaPid, Instrumentation instrumentation) {
        this.javaPid = javaPid;
        this.sessionManager = new DefaultSessionManager();
        this.commandHandler = new DefaultCommandHandler(this, instrumentation);

        Runtime.getRuntime().addShutdownHook(new Thread("ga-shutdown-hooker") {

            @Override
            public void run() {
                GaServer.this.destroy();
            }
        });

    }

    /**
     * 判断服务端是否已经启动
     *
     * @return true:服务端已经启动;false:服务端关闭
     */
    public boolean isBind() {
        return isBindRef.get();
    }


    private ServerSocketChannel serverSocketChannel = null;
    private Selector selector = null;

    /**
     * 启动Greys服务端
     *
     * @param configure 配置信息
     * @throws IOException 服务器启动失败
     */
    public void bind(Configure configure) throws IOException {
        if (!isBindRef.compareAndSet(false, true)) {
            throw new IllegalStateException("already bind");
        }

        try {

            serverSocketChannel = ServerSocketChannel.open();
            selector = Selector.open();

            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.socket().setSoTimeout(configure.getConnectTimeout());
            serverSocketChannel.socket().setReuseAddress(true);
            serverSocketChannel.register(selector, OP_ACCEPT);

            // 服务器挂载端口
            serverSocketChannel.socket().bind(new InetSocketAddress(configure.getTargetIp(), configure.getTargetPort()), 24);
            if (logger.isLoggable(INFO)) {
                logger.log(INFO, format("GaServer listened on network=%s;port=%d;timeout=%d;",
                        configure.getTargetIp(),
                        configure.getTargetPort(),
                        configure.getConnectTimeout()));
            }

            activeSelectorDaemon(selector, configure);

        } catch (IOException e) {
            unbind();
            throw e;
        }

    }

    private void activeSelectorDaemon(final Selector selector, final Configure configure) {

        final ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);

        final Thread gaServerSelectorDaemon = new Thread("ga-selector-daemon") {
            @Override
            public void run() {

                while (!isInterrupted()
                        && isBind()) {

                    try {

                        while (selector.select() > 0) {
                            final Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                            while (it.hasNext()) {
                                final SelectionKey key = it.next();
                                it.remove();

                                // do ssc accept
                                if (key.isValid() && key.isAcceptable()) {
                                    doAccept(key, selector, configure);
                                }

                                // do sc read
                                if (key.isValid() && key.isReadable()) {
                                    doRead(byteBuffer, key);
                                }

                            }
                        }

                    } catch (IOException e) {
                        if (logger.isLoggable(WARNING)) {
                            logger.log(WARNING, "selector failed.", e);
                        }
                    } catch (ClosedSelectorException e) {
                        //
                    }


                }

            }
        };
        gaServerSelectorDaemon.setDaemon(true);
        gaServerSelectorDaemon.start();
    }

    private void doAccept(SelectionKey key, Selector selector, Configure configure) throws IOException {
        final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        acceptSocketChannel(selector, serverSocketChannel, configure);
    }

    private SocketChannel acceptSocketChannel(Selector selector, ServerSocketChannel serverSocketChannel, Configure configure) throws IOException {
        final SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.socket().setSoTimeout(configure.getConnectTimeout());
        socketChannel.socket().setTcpNoDelay(true);


        final Session session = sessionManager.newSession(javaPid, socketChannel, DEFAULT_CHARSET);
        socketChannel.register(selector, OP_READ, new GaAttachment(BUFFER_SIZE, session));
        if (logger.isLoggable(INFO)) {
            logger.log(INFO, format("%s accept an connection, session[%d];client=%s;",
                    GaServer.this,
                    session.getSessionId(),
                    socketChannel));
        }

        // 这里输出Logo
        socketChannel.write(ByteBuffer.wrap(getLogo().getBytes(DEFAULT_CHARSET)));

        // 绘制提示符
        reDrawPrompt(socketChannel, session.getCharset(), session.prompt());

        return socketChannel;
    }

    private void doRead(final ByteBuffer byteBuffer, SelectionKey key) {
        final GaAttachment attachment = (GaAttachment) key.attachment();
        final SocketChannel socketChannel = (SocketChannel) key.channel();
        final Session session = attachment.getSession();
        try {

            // 若读到-1，则说明SocketChannel已经关闭
            if (-1 == socketChannel.read(byteBuffer)) {
                if (logger.isLoggable(INFO)) {
                    logger.log(INFO, format("client=%s was closed, for %s",
                            socketChannel,
                            GaServer.this));
                }
                closeSocketChannel(key, socketChannel);

                return;
            }


            // decode for line
            byteBuffer.flip();
            while (byteBuffer.hasRemaining()) {
                switch (attachment.getLineDecodeState()) {
                    case READ_CHAR: {
                        final byte data = byteBuffer.get();
                        if ('\n' == data) {
                            attachment.setLineDecodeState(READ_EOL);
                        }

                        // 遇到中止命令(CTRL_D)，则标记会话为不可写，让后台任务停下
                        else if (CTRL_D == data
                                || CTRL_X == data) {
                            session.unLock();
                            break;
                        }

                        // 普通byte则持续放入到缓存中
                        else {
                            if ('\r' != data) {
                                attachment.put(data);
                            }
                            break;
                        }

                    }

                    case READ_EOL: {
                        final String line = attachment.clearAndGetLine(session.getCharset());

                        executorService.execute(new Runnable() {
                            @Override
                            public void run() {

                                // 会话只有未锁定的时候才能响应命令
                                if (session.tryLock()) {
                                    try {

                                        // 命令执行
                                        commandHandler.executeCommand(line, session);

                                        // 命令结束之后需要传输EOT告诉client命令传输已经完结，可以展示提示符
                                        socketChannel.write(ByteBuffer.wrap(new byte[]{EOT}));

                                    } catch (IOException e) {
                                        if (logger.isLoggable(WARNING)) {
                                            logger.log(WARNING,
                                                    format("network communicate failed, session[%d] will be close.",
                                                            session.getSessionId()));
                                        }
                                        session.destroy();
                                    } finally {
                                        session.unLock();
                                    }
                                } else {
                                    if (logger.isLoggable(INFO)) {
                                        logger.log(INFO, format("session[%d] was locked, ignore this command.",
                                                session.getSessionId()));
                                    }
                                }
                            }
                        });

                        attachment.setLineDecodeState(READ_CHAR);
                        break;
                    }
                }
            }//while for line decode

            byteBuffer.clear();

        }

        // 处理
        catch (IOException e) {
            if (logger.isLoggable(WARNING)) {
                logger.log(WARNING, format("read/write data failed, session[%d] will be close",
                        session.getSessionId()), e);
            }
            closeSocketChannel(key, socketChannel);
            session.destroy();
        }
    }


    /*
     * 绘制提示符
     */
    private void reDrawPrompt(SocketChannel socketChannel, Charset charset, String prompt) throws IOException {
        final ByteBuffer buffer = ByteBuffer.wrap(prompt.getBytes(charset));
        while (buffer.hasRemaining()) {
            socketChannel.write(buffer);
        }

    }

    private void closeSocketChannel(SelectionKey key, SocketChannel socketChannel) {
        IOUtil.close(socketChannel);
        key.cancel();
    }

    /**
     * 关闭Greys服务端
     */
    public void unbind() {

        sessionManager.clean();

        IOUtil.close(serverSocketChannel);
        IOUtil.close(selector);

        if (!isBindRef.compareAndSet(true, false)) {
            throw new IllegalStateException("already unbind");
        }
    }


    public void destroy() {
        if (isBind()) {
            unbind();
        }
        sessionManager.destroy();
        executorService.shutdown();
        commandHandler.destroy();
        sessionManager.destroy();
    }

    private static volatile GaServer gaServer;

    /**
     * 单例
     *
     * @param instrumentation JVM增强
     * @return GaServer单例
     */
    public static GaServer getInstance(final int javaPid, final Instrumentation instrumentation) {
        if (null == gaServer) {
            synchronized (GaServer.class) {
                if (null == gaServer) {
                    gaServer = new GaServer(javaPid, instrumentation);
                }
            }
        }
        return gaServer;
    }

}
