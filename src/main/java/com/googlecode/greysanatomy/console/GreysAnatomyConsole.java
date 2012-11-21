package com.googlecode.greysanatomy.console;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.Writer;

import jline.console.ConsoleReader;
import jline.console.KeyMap;

import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.greysanatomy.Configer;
import com.googlecode.greysanatomy.console.command.Commands;
import com.googlecode.greysanatomy.console.network.coder.KillJobsCmd;
import com.googlecode.greysanatomy.console.network.coder.ReqCmd;
import com.googlecode.greysanatomy.console.network.coder.RespCmd;
import com.googlecode.greysanatomy.util.GaStringUtils;

/**
 * 控制台
 * @author vlinux
 *
 */
public class GreysAnatomyConsole {

	private static final Logger logger = LoggerFactory.getLogger("greysanatomy");
	
	private final Configer configer;
	private final ConsoleReader console;
	
	private volatile boolean isF = true;
	
	/**
	 * 创建GA控制台
	 * @param configer
	 * @throws IOException
	 */
	public GreysAnatomyConsole(Configer configer) throws IOException {
		this.console = new ConsoleReader(System.in, System.out);
		this.configer = configer;
		write(GaStringUtils.getLogo());
		Commands.getInstance().registCompleter(console);
	}
	
	/**
	 * 控制台输入者
	 * @author vlinux
	 *
	 */
	private class GaConsoleInputer implements Runnable {

		private final Channel channel;
		private GaConsoleInputer(Channel channel) {
			this.channel = channel;
		}
		
		@Override
		public void run() {
			while(true) {
				try {
					doRead();
				}catch(IOException e) {
					// 这里是控制台，可能么？
					logger.warn("console read failed.",e);
				}
			}
		}
		
		private void doRead() throws IOException {
			final String prompt = isF ? configer.getConsolePrompt() : EMPTY;
			final ReqCmd reqCmd = new ReqCmd(console.readLine(prompt));
			
			/*
			 * 如果读入的是空白字符串或者当前控制台没被标记为已完成
			 * 则放弃本次所读取内容
			 */
			if( isBlank(reqCmd.getCommand()) || !isF ) {
				return;
			}
			
			// 将命令状态标记为未完成
			isF = false;
			
			// 发送命令请求
			channel.write(reqCmd).awaitUninterruptibly();
			
		}
		
	}
	
	/**
	 * 启动console
	 * @param channel
	 */
	public synchronized void start(final Channel channel) {
		this.console.getKeys().bind(""+KeyMap.CTRL_D, new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				if( !isF ) {
					write("abort it.");
					isF = true;
					channel.write(new KillJobsCmd()).awaitUninterruptibly();
				}
			}
			
		});
		new Thread(new GaConsoleInputer(channel)).start();
	}
	
	
	/**
	 * 向控制台输出返回信息
	 * @param resp
	 */
	public void write(RespCmd resp) {
		if( !isF) {
			if( resp.isFinish() ) {
				isF = true;
			}
			write(resp.getMessage());
		}
	}
	
	/**
	 * 输出信息
	 * @param message
	 */
	private void write(String message) {
		final Writer writer = console.getOutput();
		try {
			writer.write(message+"\n");
			writer.flush();
		}catch(IOException e) {
			// 控制台写失败，可能么？
			logger.warn("console write failed.", e);
		}
		
	}
	
}
