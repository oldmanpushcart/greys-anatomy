package com.googlecode.greysanatomy.console.command;

import com.googlecode.greysanatomy.console.server.ConsoleServer;

import java.lang.instrument.Instrumentation;

/**
 * 抽象命令类
 *
 * @author vlinux
 */
public abstract class Command {

    /**
     * 信息发送者
     *
     * @author vlinux
     */
    public interface Sender {

        /**
         * 发送信息
         *
         * @param isF
         * @param message
         */
        void send(boolean isF, String message);

    }


    /**
     * 命令信息
     *
     * @author vlinux
     * @author chengtongda
     */
    public static class Info {

        private final Instrumentation inst;
        private final long sessionId;
        private final int jobId;

        public Info(Instrumentation inst, long sessionId, int jobId) {
            this.inst = inst;
            this.sessionId = sessionId;
            this.jobId = jobId;
        }

        public Instrumentation getInst() {
            return inst;
        }

        public long getSessionId() {
            return sessionId;
        }

        public int getJobId() {
            return jobId;
        }

    }

    /**
     * 命令动作
     *
     * @author vlinux
     */
    public interface Action {

        /**
         * 执行动作
         *
         * @param consoleServer
         * @param info
         * @param sender
         * @throws Throwable
         */
        void action(ConsoleServer consoleServer, Info info, Sender sender) throws Throwable;

    }

    /**
     * 获取命令动作
     *
     * @return
     */
    abstract public Action getAction();

}
