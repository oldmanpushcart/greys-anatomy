package com.github.ompc.greys.command;

import com.github.ompc.greys.advisor.AdviceListener;
import com.github.ompc.greys.server.Session;
import com.github.ompc.greys.util.Matcher;
import com.github.ompc.greys.util.RowAffect;

import java.lang.instrument.Instrumentation;

/**
 * 命令
 * Created by vlinux on 15/5/18.
 */
public interface Command {

    /**
     * 信息发送者
     *
     * @author vlinux
     */
    interface Sender {

        /**
         * 发送信息
         *
         * @param isF     是否结束发送
         * @param message 发送信息内容
         */
        void send(boolean isF, String message);

    }

    /**
     * 类增强
     */
    interface GetEnhancer {

        /**
         * 类名匹配
         *
         * @return 获取类名匹配
         */
        Matcher getClassNameMatcher();

        /**
         * 方法名匹配
         *
         * @return 获取方法名匹配
         */
        Matcher getMethodNameMatcher();

        /**
         * 是否包括子类
         *
         * @return 返回是否包括子类
         */
        boolean isIncludeSub();

        /**
         * 获取监听器
         *
         * @return 返回监听器
         */
        AdviceListener getAdviceListener();

    }


    /**
     * 命令动作
     */
    interface Action {


    }


    /**
     * 类增强动作
     */
    interface GetEnhancerAction extends Action {

        /**
         * 执行动作
         *
         * @param session 会话
         * @param inst    inst
         * @param sender  信息发送者
         * @return 类增强
         * @throws Throwable 动作执行出错
         */
        GetEnhancer action(Session session, Instrumentation inst, Sender sender) throws Throwable;

    }

    /**
     * 安静命令动作
     */
    interface SilentAction extends Action {

        /**
         * 安静的执行动作
         *
         * @param session 会话
         * @param inst    inst
         * @param sender  信息发送者
         * @throws Throwable 动作执行出错
         */
        void action(Session session, Instrumentation inst, Sender sender) throws Throwable;

    }


    /**
     * 影响动作
     */
    interface RowAction extends Action {

        /**
         * 安静的执行动作
         *
         * @param session 会话
         * @param inst    inst
         * @param sender  信息发送者
         * @return 影响范围
         * @throws Throwable 动作执行出错
         */
        RowAffect action(Session session, Instrumentation inst, Sender sender) throws Throwable;

    }

    /**
     * 获取命令动作
     *
     * @return 返回命令所对应的命令动作
     */
    Action getAction();

}
