package com.github.ompc.greys.core;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.util.List;

import static com.github.ompc.greys.core.util.GaStringUtils.getCauseMessage;
import static java.io.File.separator;
import static java.lang.System.getProperty;

/**
 * Greys启动器
 */
public class GreysLauncher {

    /**
     * greys' core jarfile
     */
    public static final String CORE_JARFILE =
            getProperty("user.dir") + separator + "greys-core.jar";

    /**
     * greys' agent jarfile
     */
    public static final String AGENT_JARFILE =
            getProperty("user.dir") + separator + "greys-agent.jar";


    public GreysLauncher(String[] args) throws Exception {

        // 解析配置文件
        Configure configure = analyzeConfigure(args);

        // 加载agent
        attachAgent(configure);

    }

    /*
     * 解析Configure
     */
    private Configure analyzeConfigure(String[] args) {
        final OptionParser parser = new OptionParser();
        parser.accepts("pid").withRequiredArg().ofType(int.class).required();
        parser.accepts("target").withOptionalArg().ofType(String.class);
        parser.accepts("multi").withOptionalArg().ofType(int.class);
        parser.accepts("core").withOptionalArg().ofType(String.class);
        parser.accepts("agent").withOptionalArg().ofType(String.class);
        parser.accepts("silent").withOptionalArg().ofType(boolean.class);

        final OptionSet os = parser.parse(args);
        final Configure configure = new Configure();

        if (os.has("target")) {
            final String[] strSplit = ((String) os.valueOf("target")).split(":");
            configure.setTargetIp(strSplit[0]);
            configure.setTargetPort(Integer.valueOf(strSplit[1]));
        }

        configure.setJavaPid((Integer) os.valueOf("pid"));
        configure.setGreysAgent((String) os.valueOf("agent"));
        configure.setGreysCore((String) os.valueOf("core"));
        configure.setSilent(os.has("silent"));

        return configure;
    }

    /*
     * 加载Agent
     */
    private void attachAgent(Configure configure) throws Exception {

        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final Class<?> vmdClass = loader.loadClass("com.sun.tools.attach.VirtualMachineDescriptor");
        final Class<?> vmClass = loader.loadClass("com.sun.tools.attach.VirtualMachine");

        Object attachVmdObj = null;
        for (Object obj : (List<?>) vmClass.getMethod("list", (Class<?>[]) null).invoke(null, (Object[]) null)) {
            if ((vmdClass.getMethod("id", (Class<?>[]) null).invoke(obj, (Object[]) null))
                    .equals(Integer.toString(configure.getJavaPid()))) {
                attachVmdObj = obj;
            }
        }

//        if (null == attachVmdObj) {
//            // throw new IllegalArgumentException("pid:" + configure.getJavaPid() + " not existed.");
//        }

        Object vmObj = null;
        try {
            if (null == attachVmdObj) { // 使用 attach(String pid) 这种方式
                vmObj = vmClass.getMethod("attach", String.class).invoke(null, "" + configure.getJavaPid());
            } else {
                vmObj = vmClass.getMethod("attach", vmdClass).invoke(null, attachVmdObj);
            }
            vmClass.getMethod("loadAgent", String.class, String.class).invoke(vmObj, configure.getGreysAgent(), configure.getGreysCore() + ";" + configure.toString());
        } finally {
            if (null != vmObj) {
                vmClass.getMethod("detach", (Class<?>[]) null).invoke(vmObj, (Object[]) null);
            }
        }

    }


    public static void main(String[] args) {
        try {
            new GreysLauncher(args);
        } catch (Throwable t) {
            System.err.println("start greys failed, because : " + getCauseMessage(t));
            System.exit(-1);
        }
    }
}
