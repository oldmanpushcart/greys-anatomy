package com.googlecode.greysanatomy.console.network;

import static java.util.concurrent.Executors.newCachedThreadPool;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.greysanatomy.Configer;
import com.googlecode.greysanatomy.console.GreysAnatomyConsole;
import com.googlecode.greysanatomy.console.network.coder.CmdDecoder;
import com.googlecode.greysanatomy.console.network.coder.CmdEncoder;
import com.googlecode.greysanatomy.console.network.coder.Protocol;
import com.googlecode.greysanatomy.console.network.coder.ProtocolDecoder;
import com.googlecode.greysanatomy.console.network.coder.ProtocolEncoder;
import com.googlecode.greysanatomy.exception.ConsoleException;
import com.googlecode.greysanatomy.util.JvmUtils;
import com.googlecode.greysanatomy.util.JvmUtils.ShutdownHook;

public class ConsoleClient {

	private static final Logger logger = LoggerFactory.getLogger("greysanatomy");
	
	private final ClientBootstrap bootstrap;
	private final ChannelGroup channelGroup;
	private final Channel channel;
	private final Configer configer;
	
	private ConsoleClient(Configer configer) throws ConsoleException, IOException {
		
		final GreysAnatomyConsole console = new GreysAnatomyConsole(configer);
		
		this.configer = configer;
		this.bootstrap = new ClientBootstrap(
			new NioClientSocketChannelFactory(
					newCachedThreadPool(),
					newCachedThreadPool()));
				
		this.bootstrap.setOption("child.tcpNoDelay", true);
		this.bootstrap.setOption("child.keepAlive", true);
		this.bootstrap.setOption("child.connectTimeoutMillis", configer.getConnectTimeout());
		this.bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = Channels.pipeline();
				pipeline.addLast("protocol-decoder", new ProtocolDecoder());
				pipeline.addLast("cmd-decoder", new CmdDecoder());
				pipeline.addLast("console-client-handler", new ConsoleClientHandler(console));
				pipeline.addLast("protocol-encoder", new ProtocolEncoder());
				pipeline.addLast("cmd-encoder", new CmdEncoder());
				return pipeline;
			}

		});
		this.channelGroup = new DefaultChannelGroup();
		this.channelGroup.add(this.channel = connect());
		
		JvmUtils.registShutdownHook("ga-console-client", new ShutdownHook(){

			@Override
			public void shutdown() throws Throwable {
				if( null != channelGroup ) {
					channelGroup.close();
				}
				if( null != bootstrap ) {
					bootstrap.releaseExternalResources();
				}
			}
			
		});
		
		console.start(this.channel);
		heartBeat();
		
	}
	
	/**
	 * 启动心跳侦测线程
	 */
	private void heartBeat() {
		Thread heartBeatDaemon = new Thread("ga-console-client-heartbeat"){

			@Override
			public void run() {
				while(true) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						//
					}
					if( null == channel || !channel.isConnected() ) {
						// 链接已关闭，客户端留着也没啥意思了，在这里退出JVM
						logger.info("disconnect to ga-console-server, shutdown jvm.");
						System.exit(0);
						break;
					} else {
						// 这里只用发就好了
						channel.write(Protocol.newHeartBeat());
					}
				}
			}
			
		};
		heartBeatDaemon.setDaemon(true);
		heartBeatDaemon.start();
	}
	
	/**
	 * 连接到服务器
	 * @return
	 * @throws ConsoleException
	 */
	private Channel connect() throws ConsoleException {
		final InetSocketAddress address = new InetSocketAddress(configer.getConsolePort());
		final ChannelFuture future = bootstrap.connect(address);
		future.awaitUninterruptibly();
		if( null != future.getCause() ) {
			throw new ConsoleException("init console-client failed.", future.getCause());
		}
		logger.info("connect to {} successed.", address);
		return future.getChannel();
	}
	
	
	private static ConsoleClient instance;
	
	/**
	 * 单例控制台客户端
	 * @param configer
	 * @throws ConsoleException
	 * @throws IOException 
	 */
	public static synchronized void getInstance(Configer configer) throws ConsoleException, IOException {
		if( null == instance ) {
			instance = new ConsoleClient(configer);
		}
	}
	
}
