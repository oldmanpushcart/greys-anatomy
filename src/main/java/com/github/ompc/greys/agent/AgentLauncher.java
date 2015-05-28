package com.github.ompc.greys.agent;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

/**
 * 代理启动类
 * Created by vlinux on 15/5/19.
 */
public class AgentLauncher {


    public static final String KEY_GREYS_CLASS_LOADER = "KEY_GREYS_CLASS_LOADER";
    public static final String KEY_GREYS_ADVICE_BEFORE_METHOD = "KEY_GREYS_ADVICE_BEFORE_METHOD";
    public static final String KEY_GREYS_ADVICE_RETURN_METHOD = "KEY_GREYS_ADVICE_RETURN_METHOD";
    public static final String KEY_GREYS_ADVICE_THROWS_METHOD = "KEY_GREYS_ADVICE_THROWS_METHOD";
    public static final String KEY_GREYS_ADVICE_BEFORE_INVOKING_METHOD = "KEY_GREYS_ADVICE_BEFORE_INVOKING_METHOD";
    public static final String KEY_GREYS_ADVICE_AFTER_INVOKING_METHOD = "KEY_GREYS_ADVICE_AFTER_INVOKING_METHOD";

    public static void premain(String args, Instrumentation inst) {
        main(args, inst);
    }

    public static void agentmain(String args, Instrumentation inst) {
        main(args, inst);
    }


    private static ClassLoader loadOrDefineClassLoader(String agentJar) throws Throwable {

        final Properties props = System.getProperties();
        final ClassLoader classLoader;
        if (props.containsKey(KEY_GREYS_CLASS_LOADER)) {
            classLoader = (ClassLoader) props.get(KEY_GREYS_CLASS_LOADER);
        } else {
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

            final Class<?> clazz = classLoader.loadClass("com.github.ompc.greys.advisor.AdviceWeaver");
            props.put(KEY_GREYS_CLASS_LOADER, classLoader);
            props.put(KEY_GREYS_ADVICE_BEFORE_METHOD, clazz.getMethod("methodOnBegin",
                    int.class,
                    ClassLoader.class,
                    String.class,
                    String.class,
                    String.class,
                    Object.class,
                    Object[].class));
            props.put(KEY_GREYS_ADVICE_RETURN_METHOD, clazz.getMethod("methodOnReturnEnd",
                    Object.class));
            props.put(KEY_GREYS_ADVICE_THROWS_METHOD, clazz.getMethod("methodOnThrowingEnd",
                    Throwable.class));

            props.put(KEY_GREYS_ADVICE_BEFORE_INVOKING_METHOD, clazz.getMethod("methodOnInvokeBeforeTracing",
                    int.class,
                    String.class,
                    String.class,
                    String.class));
            props.put(KEY_GREYS_ADVICE_AFTER_INVOKING_METHOD, clazz.getMethod("methodOnInvokeAfterTracing",
                    int.class,
                    String.class,
                    String.class,
                    String.class));
        }

        return classLoader;
    }

    private static synchronized void main(final String args, final Instrumentation inst) {
        try {

            // 传递的args参数分两个部分:agentJar路径和agentArgs
            // 分别是Agent的JAR包路径和期望传递到服务端的参数
            final int index = args.indexOf(";");
            final String agentJar = args.substring(0, index);
            final String agentArgs = args.substring(index, args.length());

            // 构造自定义的类加载器，尽量减少Greys对现有工程的侵蚀
            final ClassLoader agentLoader = loadOrDefineClassLoader(agentJar);


            // Configure类定义
            final Class<?> classOfConfigure = agentLoader.loadClass("com.github.ompc.greys.Configure");

            // GaServer类定义
            final Class<?> classOfGaServer = agentLoader.loadClass("com.github.ompc.greys.server.GaServer");

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
