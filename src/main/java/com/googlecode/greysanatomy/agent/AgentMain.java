package com.googlecode.greysanatomy.agent;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;

import com.googlecode.greysanatomy.Configer;
import com.googlecode.greysanatomy.GreysAnatomyMain;

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
			URLClassLoader agentLoader = new URLClassLoader(new URL[]{new URL("file:"+GreysAnatomyMain.JARFILE)});
			
			final Configer configer = Configer.toConfiger(args);
			agentLoader.loadClass("com.googlecode.greysanatomy.console.network.ConsoleServer").getMethod("getInstance",Configer.class, Instrumentation.class).invoke(null, configer, inst);
			
		}catch(Throwable t) {
			t.printStackTrace();
		}
		
	}
	
}
