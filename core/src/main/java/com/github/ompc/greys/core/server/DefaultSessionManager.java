package com.github.ompc.greys.core.server;

import com.github.ompc.greys.core.util.LogUtil;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 默认会话管理器实现
 * Created by oldmanpushcart@gmail.com on 15/5/2.
 */
public class DefaultSessionManager implements SessionManager {

    private final Logger logger = LogUtil.getLogger();

    // 5分钟
    private static final long DURATION_5_MINUTE = 5L * 60 * 1000;

    // 会话超时时间
    private static final long DEFAULT_SESSION_DURATION = DURATION_5_MINUTE;

    // 会话管理Map
    private final ConcurrentHashMap<Integer, Session> sessionMap = new ConcurrentHashMap<Integer, Session>();

    // 会话ID序列生成器
    private final AtomicInteger sessionIndexSequence = new AtomicInteger(0);

    private final AtomicBoolean isDestroyRef = new AtomicBoolean(false);

    public DefaultSessionManager() {
        activeSessionExpireDaemon();
    }

    @Override
    public Session getSession(int sessionId) {
        return sessionMap.get(sessionId);
    }

    @Override
    public Session newSession(int javaPid, SocketChannel socketChannel, Charset charset) {
        final int sessionId = sessionIndexSequence.getAndIncrement();
        final Session session = new Session(javaPid, sessionId, DEFAULT_SESSION_DURATION, socketChannel, charset) {
            @Override
            public void destroy() {
                super.destroy();
                sessionMap.remove(sessionId);
            }
        };

        final Session sessionInMap = sessionMap.putIfAbsent(sessionId, session);
        if (null != sessionInMap) {
            // 之前竟然存在，返回之前的
            return sessionInMap;
        }

        return session;
    }

    /**
     * 激活会话过期管理守护线程
     */
    private void activeSessionExpireDaemon() {
        final Thread sessionExpireDaemon = new Thread("ga-session-expire-daemon") {

            @Override
            public void run() {
                while (!isDestroyRef.get()
                        && !isInterrupted()) {

                    // 间隔200ms检测一次
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        interrupt();
                    }

                    for (final Map.Entry<Integer, Session> entry : sessionMap.entrySet()) {

                        final int sessionId = entry.getKey();
                        final Session session = entry.getValue();
                        if (null == session
                                || session.isExpired()) {

                            logger.info("session[{}] was expired", sessionId);

                            if (null != session) {

                                try {
                                    // 会话超时，关闭之前输出超时信息
                                    session.getSocketChannel().write(ByteBuffer.wrap("Session timed out.\n".getBytes(session.getCharset())));
                                } catch (IOException e) {
                                    logger.debug("write expired message to session[{}] failed.", sessionId, e);
                                }

                                session.destroy();
                            }

                            sessionMap.remove(sessionId);

                        }

                    }

                }
            }
        };
        sessionExpireDaemon.setDaemon(true);
        sessionExpireDaemon.start();
    }


    @Override
    public void clean() {
        // shutdown all the session
        for (Session session : sessionMap.values()) {
            session.destroy();
        }

        sessionMap.clear();

        logger.info("session manager clean completed.");

    }

    @Override
    public boolean isDestroy() {
        return isDestroyRef.get();
    }

    @Override
    public void destroy() {

        if (!isDestroyRef.compareAndSet(false, true)) {
            throw new IllegalStateException("already destroy");
        }

        clean();

        logger.info("session manager destroy completed.");

    }
}
