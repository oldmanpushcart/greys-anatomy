package com.googlecode.greysanatomy.console.network.coder;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;


/**
 * Protocol±àÂëÆ÷
 * @author vlinux
 *
 */
public class ProtocolEncoder extends OneToOneEncoder {

	@Override
	protected Object encode(ChannelHandlerContext ctx, Channel channel,
			Object msg) throws Exception {
		
		if( null == msg
				|| !(msg instanceof Protocol) ) {
			return msg;
		}
		
		Protocol protocol = (Protocol)msg;
		
		ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
		buffer.writeShort(Protocol.MAGIC);
		buffer.writeByte(protocol.getType());
		buffer.writeInt(protocol.getLength());
		buffer.writeBytes(protocol.getDatas());
		
		return buffer;
	}

}
