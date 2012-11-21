package com.googlecode.greysanatomy;

import java.io.IOException;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.greysanatomy.console.network.ConsoleClient;
import com.googlecode.greysanatomy.exception.ConsoleException;
import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

/**
 * Hello world!
 * 
 */
public class GreysAnatomyMain {
	
	private static final Logger logger = LoggerFactory.getLogger("greysanatomy");
	public static final String JARFILE = GreysAnatomyMain.class.getProtectionDomain().getCodeSource().getLocation().getFile();
	
	public GreysAnatomyMain(String[] args) throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException, ConsoleException {
		
		// 解析配置文件
		Configer configer = parsetConfiger(args);
		
		// 加载agent
		attachAgent(configer);
		
		// 激活控制台
		activeConsoleClient(configer);
		
		logger.info("attach done! pid={}; port={}; JarFile={}", new Object[]{
				configer.getJavaPid(), 
				configer.getConsolePort(), 
				JARFILE});
	}
	
	/**
	 * 解析configer
	 * @param args
	 * @return
	 */
	private Configer parsetConfiger(String[] args) {
		final OptionParser parser = new OptionParser();
		parser.accepts("pid").withRequiredArg().ofType(int.class).required();
		parser.accepts("port").withOptionalArg().ofType(int.class);
		final OptionSet os = parser.parse(args);
		
		final Configer configer = new Configer();
		if( os.has("port") ) {
			configer.setConsolePort((Integer)os.valueOf("port"));
		}
		configer.setJavaPid((Integer)os.valueOf("pid"));
		return configer;
	}
	
	/**
	 * 加载Agent
	 * @param configer
	 * @throws AttachNotSupportedException
	 * @throws IOException
	 * @throws AgentLoadException
	 * @throws AgentInitializationException
	 */
	private void attachAgent(Configer configer) throws AttachNotSupportedException, IOException, AgentLoadException, AgentInitializationException {
		VirtualMachineDescriptor attachVmd = null;
		for( VirtualMachineDescriptor vmd : VirtualMachine.list() ) {
			if( vmd.id().equals(""+configer.getJavaPid()) ) {
				attachVmd = vmd;
				break;
			}
		}//for
		
		if( null == attachVmd ) {
			throw new IllegalArgumentException("pid:"+configer.getJavaPid()+" not existed.");
		}
		
		VirtualMachine vm = null;
		try {
			vm = VirtualMachine.attach(attachVmd);
			vm.loadAgent(JARFILE, configer.toString());
		}finally {
			if( null != vm ) {
				vm.detach();
			}
		}
	}
	
	/**
	 * 激活控制台客户端
	 * @param configer
	 * @throws ConsoleException
	 * @throws IOException
	 */
	private void activeConsoleClient(Configer configer) throws ConsoleException, IOException {
		ConsoleClient.getInstance(configer);
	}
	
	
	
	public static void main(String[] args)  {
		
		try {
			new GreysAnatomyMain(args);
		}catch(Throwable t) {
			logger.error("start greys-anatomy failed.",t);
			System.exit(-1);
		}
		
	}
}
