package com.github.ompc.greys.agent;

import java.lang.reflect.Method;

/**
 * 间谍类<br/>
 * 藏匿在各个ClassLoader中
 * Created by vlinux on 15/8/23.
 */
public class Spy {


    // -- 各种Advice的钩子引用 --
    public static volatile Method ON_BEFORE_METHOD;
    public static volatile Method ON_RETURN_METHOD;
    public static volatile Method ON_THROWS_METHOD;
    public static volatile Method BEFORE_INVOKING_METHOD;
    public static volatile Method AFTER_INVOKING_METHOD;

    /**
     * greys's classloader 引用
     */
    public static volatile ClassLoader CLASSLOADER;

    /**
     * 代理重设方法
     */
    public static volatile Method AGENT_RESET_METHOD;

    /*
     * 用于普通的间谍初始化
     */
    public static void init(
            ClassLoader classLoader,
            Method onBeforeMethod,
            Method onReturnMethod,
            Method onThrowsMethod,
            Method beforeInvokingMethod,
            Method afterInvokingMethod) {
        CLASSLOADER = classLoader;
        ON_BEFORE_METHOD = onBeforeMethod;
        ON_RETURN_METHOD = onReturnMethod;
        ON_THROWS_METHOD = onThrowsMethod;
        BEFORE_INVOKING_METHOD = beforeInvokingMethod;
        AFTER_INVOKING_METHOD = afterInvokingMethod;
    }

    /*
     * 用于启动线程初始化
     */
    public static void initForAgentLauncher(
            ClassLoader classLoader,
            Method onBeforeMethod,
            Method onReturnMethod,
            Method onThrowsMethod,
            Method beforeInvokingMethod,
            Method afterInvokingMethod,
            Method agentResetMethod) {
        CLASSLOADER = classLoader;
        ON_BEFORE_METHOD = onBeforeMethod;
        ON_RETURN_METHOD = onReturnMethod;
        ON_THROWS_METHOD = onThrowsMethod;
        BEFORE_INVOKING_METHOD = beforeInvokingMethod;
        AFTER_INVOKING_METHOD = afterInvokingMethod;
        AGENT_RESET_METHOD = agentResetMethod;
    }

}
