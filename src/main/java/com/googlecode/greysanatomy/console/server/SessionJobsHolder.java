package com.googlecode.greysanatomy.console.server;

import com.googlecode.greysanatomy.console.rmi.req.GaSession;
import com.googlecode.greysanatomy.exception.SessionTimeOutException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.googlecode.greysanatomy.probe.ProbeJobs.killJob;

/**
 * 服务端当前连接上session的job持有信息
 *
 * @author chengtongda
 */
public class SessionJobsHolder {

    private static final Logger logger = LoggerFactory.getLogger("greysanatomy");

    // 会话信息
    private final static Map<Long, GaSession> sessionHolder = new ConcurrentHashMap<Long, GaSession>();

    static {
        //session生存检测
        Thread sessionHearCheck = new Thread("ga-console-server-heartCheck") {

            private final long tip = 3000;

            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(tip);
                    } catch (InterruptedException e) {
                        //
                    }
                    long currentMills = System.currentTimeMillis();
                    Set<Long> deadSessionIds = new HashSet<Long>();
                    for (GaSession session : sessionHolder.values()) {
                        //如果已经超过session失效阀值，则kill掉session
                        if (currentMills - session.getLastModified() > tip) {
                            deadSessionIds.add(session.getSessionId());
                        }
                    }
                    //将失效的session全部kill掉
                    for (Long deadSessionId : deadSessionIds) {
                        unRegistSession(deadSessionId);
                    }
                }
            }

        };
        sessionHearCheck.setDaemon(true);
        sessionHearCheck.start();
    }

    /**
     * 注册一个会话
     */
    public static synchronized long registSession() {
        GaSession session = new GaSession();
        sessionHolder.put(session.getSessionId(), session);
        logger.info("regist session={}", session.getSessionId());
        return session.getSessionId();
    }

    /**
     * session心跳
     *
     * @param gaSessionId
     * @return false为session已失效
     */
    public static synchronized boolean heartBeatSession(long gaSessionId) {
        GaSession holderSession = sessionHolder.get(gaSessionId);
        //注销任务则不需要判断会话是否还在，即使不在也可以注销
        if (holderSession == null || !holderSession.isAlive()) {
            return false;
        }
        holderSession.setLastModified(System.currentTimeMillis());
        return true;
    }

    /**
     * 注销一个任务
     *
     * @param gaSessionId
     * @param jobId
     */
<<<<<<< HEAD
    public static synchronized void unRegistJob(long gaSessionId, String jobId) {
        GaSession holderSession = sessionHolder.get(gaSessionId);
        //注销任务则不需要判断会话是否还在，即使不在也可以注销
        if (holderSession != null) {
            final Iterator<String> it = holderSession.getJobIds().iterator();
            while (it.hasNext()) {
                String id = it.next();
                if (StringUtils.equals(id, jobId)) {
=======
    public static synchronized void unRegistJob(long gaSessionId, int jobId) {
        GaSession holderSession = sessionHolder.get(gaSessionId);
        //注销任务则不需要判断会话是否还在，即使不在也可以注销
        if (holderSession != null) {
            final Iterator<Integer> it = holderSession.getJobIds().iterator();
            while (it.hasNext()) {
                final int id = it.next();
//                if (StringUtils.equals(id, jobId)) {
                if( id == jobId ) {
>>>>>>> pr/8
                    killJob(id);
                    it.remove();
                    logger.info("unRegist job={} for session={}", id, gaSessionId);
                }
            }
        }
    }

    /**
     * 注册一个job
     *
     * @param sessionId
     * @param jobId
     * @throws SessionTimeOutException
     */
<<<<<<< HEAD
    public static synchronized void registJob(long sessionId, String jobId) throws SessionTimeOutException {
=======
    public static synchronized void registJob(long sessionId, int jobId) throws SessionTimeOutException {
>>>>>>> pr/8
        GaSession holderSession = sessionHolder.get(sessionId);
        if (holderSession == null || !holderSession.isAlive()) {
            throw new SessionTimeOutException("session is not exsit!");
        }
        holderSession.getJobIds().add(jobId);
        logger.info("regist job={} for session={}", jobId, sessionId);
    }

    /**
     * 注销一个会话
     *
     * @param gaSessionId
     */
    public static synchronized void unRegistSession(long gaSessionId) {
        GaSession holderSession = sessionHolder.get(gaSessionId);
        if (holderSession == null) {
            return;
        }
        holderSession.setAlive(false);
<<<<<<< HEAD
        final Iterator<String> it = holderSession.getJobIds().iterator();
=======
        final Iterator<Integer> it = holderSession.getJobIds().iterator();
>>>>>>> pr/8
        while (it.hasNext()) {
            killJob(it.next());
        }
        sessionHolder.remove(gaSessionId);
        logger.info("unRegist session={}", gaSessionId);
    }
}
