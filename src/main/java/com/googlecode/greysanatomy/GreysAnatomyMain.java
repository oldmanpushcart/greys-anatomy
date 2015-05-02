package com.googlecode.greysanatomy;

import com.googlecode.greysanatomy.util.GaStringUtils;
import com.googlecode.greysanatomy.util.HostUtils;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Hello world!
 */
public class GreysAnatomyMain {


    public static final String JARFILE = GreysAnatomyMain.class.getProtectionDomain().getCodeSource().getLocation().getFile();

    public GreysAnatomyMain(String[] args) throws Exception {

        // 解析配置文件
        Configure configure = analyzeConfigure(args);

        // 如果是本地IP,则尝试加载Agent
        if (HostUtils.isLocalHostIp(configure.getTargetIp())) {
            // 加载agent
            attachAgent(configure);
        }

    }

    /**
     * 解析configer
     *
     * @param args
     * @return
     */
    private Configure analyzeConfigure(String[] args) {
        final OptionParser parser = new OptionParser();
        parser.accepts("pid").withRequiredArg().ofType(int.class).required();
        parser.accepts("target").withOptionalArg().ofType(String.class);
        parser.accepts("multi").withOptionalArg().ofType(int.class);

        final OptionSet os = parser.parse(args);
        final Configure configure = new Configure();

        if (os.has("target")) {
            final String[] strSplit = ((String) os.valueOf("target")).split(":");
            configure.setTargetIp(strSplit[0]);
            configure.setTargetPort(Integer.valueOf(strSplit[1]));
        }

//        if (os.has("multi")
//                && (Integer) os.valueOf("multi") == 1) {
//            configure.setMulti(true);
//        } else {
//            configure.setMulti(false);
//        }

        configure.setJavaPid((Integer) os.valueOf("pid"));
        return configure;
    }

    /**
     * 加载Agent
     *
     * @param configure
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws SecurityException
     * @throws IllegalArgumentException
     */
    private void attachAgent(Configure configure) throws IOException, ClassNotFoundException, IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {

        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final Class<?> vmdClass = loader.loadClass("com.sun.tools.attach.VirtualMachineDescriptor");
        final Class<?> vmClass = loader.loadClass("com.sun.tools.attach.VirtualMachine");

        Object attachVmdObj = null;
        for (Object obj : (List<?>) vmClass.getMethod("list", (Class<?>[]) null).invoke(null, (Object[]) null)) {
            if (((String) vmdClass.getMethod("id", (Class<?>[]) null).invoke(obj, (Object[]) null)).equals("" + configure.getJavaPid())) {
                attachVmdObj = obj;
            }
        }

        if (null == attachVmdObj) {
            // throw new IllegalArgumentException("pid:" + configure.getJavaPid() + " not existed.");
        }

        Object vmObj = null;
        try {
            if (null == attachVmdObj) { // 使用 attach(String pid) 这种方式
                vmObj = vmClass.getMethod("attach", String.class).invoke(null, "" + configure.getJavaPid());
            } else {
                vmObj = vmClass.getMethod("attach", vmdClass).invoke(null, attachVmdObj);
            }
            vmClass.getMethod("loadAgent", String.class, String.class).invoke(vmObj, JARFILE, configure.toString());
        } finally {
            if (null != vmObj) {
                vmClass.getMethod("detach", (Class<?>[]) null).invoke(vmObj, (Object[]) null);
            }
        }

    }

    public static void main(String[] args) {

        try {
            new GreysAnatomyMain(args);
        } catch (Throwable t) {
//            error(t, "start greys-anatomy failed. because %s", t.getMessage());
            System.err.println("start greys failed, because : " + GaStringUtils.getCauseMessage(t));
            System.exit(-1);
        }

    }
}
