package com.github.ompc.greys.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * 代理启动类
 * Created by vlinux on 15/5/19.
 */
public class AgentLauncher {

    // 全局持有classloader用于隔离greys实现
    private static ClassLoader greysClassLoader;

    public static void premain(String args, Instrumentation inst) {
        main(args, inst);
    }

    public static void agentmain(String args, Instrumentation inst) {
        main(args, inst);
    }


    /**
     * 重置greys的classloader<br/>
     * 让下次再次启动时有机会重新加载
     */
    public synchronized static void resetGreysClassLoader() {
        greysClassLoader = null;
    }

    private static ClassLoader loadOrDefineClassLoader(String agentJar) throws Throwable {

        final ClassLoader classLoader;

        // 如果已经被启动则返回之前启动的classloader
        if (null != greysClassLoader) {
            classLoader = greysClassLoader;
        }

        // 如果未启动则重新加载
        else {
            classLoader = new URLClassLoader(new URL[]{new URL("file:" + agentJar)}) {

                @Override
                protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                    final Class<?> loadedClass = findLoadedClass(name);
                    if (loadedClass != null) {
                        return loadedClass;
                    }

                    try {
                        Class<?> aClass = findClass(name);
                        if (resolve) {
                            resolveClass(aClass);
                        }
                        return aClass;
                    } catch (Exception e) {
                        return super.loadClass(name, resolve);
                    }
                }

            };

            // 获取各种Hook
            final Class<?> adviceWeaverClass = classLoader.loadClass("com.github.ompc.greys.core.advisor.AdviceWeaver");
            final Class<?> spyClass = classLoader.loadClass("com.github.ompc.greys.core.advisor.Spy");
            final Method spySetMethod = spyClass.getMethod("set",
                    ClassLoader.class,
                    Method.class,
                    Method.class,
                    Method.class,
                    Method.class,
                    Method.class);

            spySetMethod.invoke(null,
                    classLoader,
                    adviceWeaverClass.getMethod("methodOnBegin",
                            int.class,
                            ClassLoader.class,
                            String.class,
                            String.class,
                            String.class,
                            Object.class,
                            Object[].class),
                    adviceWeaverClass.getMethod("methodOnReturnEnd",
                            Object.class),
                    adviceWeaverClass.getMethod("methodOnThrowingEnd",
                            Throwable.class),
                    adviceWeaverClass.getMethod("methodOnInvokeBeforeTracing",
                            int.class,
                            String.class,
                            String.class,
                            String.class),
                    adviceWeaverClass.getMethod("methodOnInvokeAfterTracing",
                            int.class,
                            String.class,
                            String.class,
                            String.class)
                    );
        }

        return greysClassLoader = classLoader;
    }

    private static synchronized void main(final String args, final Instrumentation inst) {
        try {

            // 传递的args参数分两个部分:agentJar路径和agentArgs
            // 分别是Agent的JAR包路径和期望传递到服务端的参数
            final int index = args.indexOf(';');
            final String agentJar = args.substring(0, index);
            final String agentArgs = args.substring(index, args.length());

            // 构造自定义的类加载器，尽量减少Greys对现有工程的侵蚀
            final ClassLoader agentLoader = loadOrDefineClassLoader(agentJar);

            // Configure类定义
            final Class<?> classOfConfigure = agentLoader.loadClass("com.github.ompc.greys.core.Configure");

            // GaServer类定义
            final Class<?> classOfGaServer = agentLoader.loadClass("com.github.ompc.greys.core.server.GaServer");

            // 反序列化成Configure类实例
            final Object objectOfConfigure = classOfConfigure.getMethod("toConfigure", String.class)
                    .invoke(null, agentArgs);

            // JavaPid
            final int javaPid = (Integer) classOfConfigure.getMethod("getJavaPid").invoke(objectOfConfigure);

            // 获取GaServer单例
            final Object objectOfGaServer = classOfGaServer
                    .getMethod("getInstance", int.class, Instrumentation.class)
                    .invoke(null, javaPid, inst);

            // gaServer.isBind()
            final boolean isBind = (Boolean) classOfGaServer.getMethod("isBind").invoke(objectOfGaServer);

            if (!isBind) {
                classOfGaServer.getMethod("bind", classOfConfigure).invoke(objectOfGaServer, objectOfConfigure);
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }

    }

}
