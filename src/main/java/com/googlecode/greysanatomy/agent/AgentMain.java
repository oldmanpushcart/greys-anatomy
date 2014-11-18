package com.googlecode.greysanatomy.agent;

import com.googlecode.greysanatomy.Configer;
import com.googlecode.greysanatomy.GreysAnatomyMain;
import com.googlecode.greysanatomy.console.server.ConsoleServer;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;

public class AgentMain {

    public static void premain(String args, Instrumentation inst) {
        main(args, inst);
    }

    public static void agentmain(String args, Instrumentation inst) {
        main(args, inst);
    }

    public static synchronized void main(final String args, final Instrumentation inst) {
//		AgentServer.init(inst, ConfigUtils.DEFAULT_AGENT_SERVER_PORT);
        try {

            // 这里考虑下是否要破坏双亲委派
            URLClassLoader agentLoader = new URLClassLoader(new URL[]{new URL("file:" + GreysAnatomyMain.JARFILE)});

            final Configer configer = Configer.toConfiger(args);
<<<<<<< HEAD
            final ConsoleServer consoleServer = (ConsoleServer)agentLoader
=======
            final ConsoleServer consoleServer = (ConsoleServer) agentLoader
>>>>>>> pr/8
                    .loadClass("com.googlecode.greysanatomy.console.server.ConsoleServer")
                    .getMethod("getInstance", Configer.class, Instrumentation.class)
                    .invoke(null, configer, inst);

<<<<<<< HEAD
            if( !consoleServer.isBind() ) {
=======
            if (!consoleServer.isBind()) {
>>>>>>> pr/8
//                consoleServer.getConfiger().setTargetIp(configer.getTargetIp());
                consoleServer.getConfiger().setTargetPort(configer.getTargetPort());
                consoleServer.rebind();
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }

    }

}
