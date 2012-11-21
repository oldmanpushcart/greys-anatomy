package com.googlecode.greysanatomy.console.network;

import static java.util.concurrent.Executors.newCachedThreadPool;

import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.greysanatomy.Configer;
import com.googlecode.greysanatomy.console.network.coder.CmdDecoder;
import com.googlecode.greysanatomy.console.network.coder.CmdEncoder;
import com.googlecode.greysanatomy.console.network.coder.ProtocolDecoder;
import com.googlecode.greysanatomy.console.network.coder.ProtocolEncoder;
import com.googlecode.greysanatomy.util.GaStringUtils;
import com.googlecode.greysanatomy.util.JvmUtils;
import com.googlecode.greysanatomy.util.JvmUtils.ShutdownHook;

/**
 * 控制台服务器
 * @author vlinux
 *
 */
public class ConsoleServer {

	private static final Logger logger = LoggerFactory.getLogger("greysanatomy");
	
	private final ServerBootstrap bootstrap;
	private final ChannelGroup channelGroup;
	
	/**
	 * 构造控制台服务器
	 * @param configer
	 * @param inst
	 */
	private ConsoleServer(Configer configer, final Instrumentation inst) {
		this.bootstrap = new ServerBootstrap(
			new NioServerSocketChannelFactory(
				newCachedThreadPool(),
				newCachedThreadPool()));
		this.bootstrap.setOption("child.tcpNoDelay", true);
		this.bootstrap.setOption("child.keepAlive", true);
		this.channelGroup = new DefaultChannelGroup();
		this.bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = Channels.pipeline();
				pipeline.addLast("protocol-decoder", new ProtocolDecoder());
				pipeline.addLast("cmd-decoder", new CmdDecoder());
				pipeline.addLast("console-server-handler", new ConsoleServerHandler(inst, channelGroup));
				pipeline.addLast("protocol-encoder", new ProtocolEncoder());
				pipeline.addLast("cmd-encoder", new CmdEncoder());
				return pipeline;
			}

		});
		this.channelGroup.add(bootstrap.bind(new InetSocketAddress(configer.getConsolePort())));
		
		logger.info("ga-console-server was started at port={}", configer.getConsolePort());
		JvmUtils.registShutdownHook("ga-console-server", new ShutdownHook(){

			@Override
			public void shutdown() throws Throwable {
				if( null != channelGroup ) {
					channelGroup.close().awaitUninterruptibly();
				}
				if( null != bootstrap ) {
					bootstrap.releaseExternalResources();
				}
			}
			
		});
		
	}
	
	
	private static ConsoleServer instance;
	
	/**
	 * 单例控制台服务器
	 * @param configer
	 */
	public static synchronized ConsoleServer getInstance(Configer configer, Instrumentation inst) {
		if( null == instance ) {
			instance = new ConsoleServer(configer, inst);
			logger.info(GaStringUtils.getLogo());
		}
		return instance;
	}
	
}
