package com.googlecode.greysanatomy.console.command;

import java.lang.instrument.Instrumentation;

import org.jboss.netty.channel.Channel;

/**
 * 抽象命令类
 * @author vlinux
 *
 */
public abstract class Command {

	/**
	 * 信息发送者
	 * @author vlinux
	 *
	 */
	public static interface Sender {
		
		/**
		 * 发送信息
		 * @param isF
		 * @param message
		 */
		void send(boolean isF, String message);
		
	}
	
	
	/**
	 * 命令信息
	 * @author vlinux
	 *
	 */
	public static class Info {
		
		private final Instrumentation inst;
		private final Channel channel;
		
		public Info(Instrumentation inst, Channel channel) {
			this.inst = inst;
			this.channel = channel;
		}

		public Instrumentation getInst() {
			return inst;
		}

		public Channel getChannel() {
			return channel;
		}
		
	}
	
	/**
	 * 命令动作
	 * @author vlinux
	 *
	 */
	public interface Action {

		/**
		 * 执行动作
		 * @param info
		 * @param sender
		 * @throws Throwable
		 */
		void action(Info info, Sender sender) throws Throwable;
		
	}
	
	/**
	 * 获取命令动作
	 * @return
	 */
	abstract public Action getAction() ;
	
	
}
