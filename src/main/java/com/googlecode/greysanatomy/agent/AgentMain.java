package com.googlecode.greysanatomy.agent;

import com.googlecode.greysanatomy.Configure;
import com.googlecode.greysanatomy.GreysAnatomyMain;
import com.googlecode.greysanatomy.server.GaServer;
import com.googlecode.greysanatomy.util.LogUtils;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AgentMain {

    private static final Logger logger = LogUtils.getLogger();

    public static void premain(String args, Instrumentation inst) {
        main(args, inst);
    }

    public static void agentmain(String args, Instrumentation inst) {
        main(args, inst);
    }

    public static synchronized void main(final String args, final Instrumentation inst) {
        try {

            // 这里考虑下是否要破坏双亲委派
            URLClassLoader agentLoader = new URLClassLoader(new URL[]{new URL("file:" + GreysAnatomyMain.JARFILE)});

            final Configure configure = Configure.toConfigure(args);
            final GaServer gaServer = (GaServer) agentLoader
                    .loadClass("com.googlecode.greysanatomy.server.GaServer")
                    .getMethod("getInstance", Configure.class, Instrumentation.class)
                    .invoke(null, configure, inst);

            if (!gaServer.isBind()) {
                gaServer.bind();
            } else {
                if (logger.isLoggable(Level.INFO)) {
                    logger.log(Level.INFO, "greys server already bind : "+gaServer);
                }
            }

//            if (!consoleServer.isBind()) {
//                consoleServer.getConfigure().setTargetPort(configure.getTargetPort());
//                consoleServer.rebind();
//            }

        } catch (Throwable t) {

            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, "greys agent main failed.", t);
            }

        }

    }

}
