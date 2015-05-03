package com.googlecode.greysanatomy.server;

import com.googlecode.greysanatomy.util.LogUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * 默认会话管理器实现
 * Created by vlinux on 15/5/2.
 */
public class DefaultGaSessionManager implements GaSessionManager {

    private final Logger logger = LogUtils.getLogger();

    // 5分钟
    private static final long DURATION_5_MINUTE = 5L * 60 * 1000;

    // 会话超时时间
    private static final long DEFAULT_SESSION_DURATION = DURATION_5_MINUTE;

    // 会话管理Map
    private final ConcurrentHashMap<Integer, GaSession> gaSessionMap = new ConcurrentHashMap<Integer, GaSession>();

    // 会话ID序列生成器
    private final AtomicInteger gaSessionIndexSequence = new AtomicInteger(0);

    private final AtomicBoolean isDestroyRef = new AtomicBoolean(false);

    public DefaultGaSessionManager() {
        activeGaSessionExpireDaemon();
    }

    @Override
    public GaSession getGaSession(int gaSessionId) {
        return gaSessionMap.get(gaSessionId);
    }

    @Override
    public GaSession newGaSession(SocketChannel socketChannel, Charset charset) {
        final int gaSessionId = gaSessionIndexSequence.getAndIncrement();
        final GaSession gaSession = new GaSession(gaSessionId, DEFAULT_SESSION_DURATION, socketChannel, charset) {
            @Override
            public void destroy() {
                super.destroy();
                gaSessionMap.remove(gaSessionId);
            }
        };

        final GaSession gaSessionInMap = gaSessionMap.putIfAbsent(gaSessionId, gaSession);
        if (null != gaSessionInMap) {
            // 之前竟然存在，返回之前的
            return gaSessionInMap;
        }

        return gaSession;
    }

    /**
     * 激活会话过期管理守护线程
     */
    private void activeGaSessionExpireDaemon() {
        final Thread gaSessionExpireDaemon = new Thread("GaSession-Expire-Daemon") {

            @Override
            public void run() {
                while (!isDestroyRef.get()
                        && !isInterrupted()) {

                    // 间隔500ms检测一次
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        interrupt();
                    }

                    for (final Map.Entry<Integer, GaSession> entry : gaSessionMap.entrySet()) {

                        final int gaSessionId = entry.getKey();
                        final GaSession gaSession = entry.getValue();
                        if (null == gaSession
                                || gaSession.isExpired()) {
                            if (logger.isLoggable(Level.INFO)) {
                                logger.log(Level.INFO, format("GaSession[%d] was expired.", gaSessionId));
                            }

                            if (null != gaSession) {

                                try {
                                    // 会话超时，关闭之前输出超时信息
                                    gaSession.getSocketChannel().write(ByteBuffer.wrap("session expired.\n".getBytes()));
                                } catch (IOException e) {
                                    final String message = format("write expired message failed. sessionId=%d;", gaSession.getSessionId());
                                    if (logger.isLoggable(Level.FINE)) {
                                        logger.log(Level.FINE, message, e);
                                    }
                                    if (logger.isLoggable(Level.INFO)) {
                                        logger.log(Level.INFO, message);
                                    }
                                }

                                gaSession.destroy();
                            }

                            gaSessionMap.remove(gaSessionId);

                        }

                    }

                }
            }
        };
        gaSessionExpireDaemon.setDaemon(true);
        gaSessionExpireDaemon.start();
    }


    @Override
    public void clean() {
        // shutdown all the gaSession
        for (GaSession gaSession : gaSessionMap.values()) {
            gaSession.destroy();
        }
    }

    @Override
    public void destroy() {

        if (!isDestroyRef.compareAndSet(false, true)) {
            throw new IllegalStateException("already destroy");
        }

        clean();

    }
}
