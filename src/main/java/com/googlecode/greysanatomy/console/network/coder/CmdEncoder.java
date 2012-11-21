package com.googlecode.greysanatomy.console.network.coder;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import com.googlecode.greysanatomy.console.network.serializer.Serializer;
import com.googlecode.greysanatomy.console.network.serializer.SerializerFactory;

/**
 * cmd–≠“È±‡¬Î∆˜
 * @author vlinux
 *
 */
public class CmdEncoder extends OneToOneEncoder {

	private final Serializer serializer = SerializerFactory.getInstance();
	
	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel,
			Object msg) throws Exception {
		
		if( ! (msg instanceof CmdTracer) ) {
			return msg;
		}
		
		CmdTracer cmd = (CmdTracer)msg;
		Protocol pro = new Protocol();
		pro.setType(Protocol.TYPE_CMD);
		byte[] datas = serializer.encode(cmd);
		pro.setLength(datas.length);
		pro.setDatas(datas);
		
		return pro;
	}

}
