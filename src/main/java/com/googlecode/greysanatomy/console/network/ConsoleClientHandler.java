package com.googlecode.greysanatomy.console.network;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import com.googlecode.greysanatomy.console.GreysAnatomyConsole;
import com.googlecode.greysanatomy.console.network.coder.RespCmd;

/**
 * 控制台客户端处理器
 * @author vlinux
 *
 */
public class ConsoleClientHandler extends SimpleChannelUpstreamHandler {

	private GreysAnatomyConsole console;
	
	public ConsoleClientHandler(GreysAnatomyConsole console) {
		this.console = console;
	}
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {

		if( null == e.getMessage()
				|| !(e.getMessage() instanceof RespCmd)) {
			super.messageReceived(ctx, e);
		}
		
		final RespCmd respCmd = (RespCmd)e.getMessage();
		console.write(respCmd);
		
	}
	
}
