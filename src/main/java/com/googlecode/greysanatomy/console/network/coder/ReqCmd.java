package com.googlecode.greysanatomy.console.network.coder;

/**
 * 请求命令
 * @author vlinux
 *
 */
public class ReqCmd extends CmdTracer {

	private static final long serialVersionUID = 7156731632312708537L;

	/*
	 * 请求命令原始字符串
	 */
	private final String command;
	
	/**
	 * 请求命令构造函数
	 * @param command
	 */
	public ReqCmd(String command) {
		this.command = command;
	}

	/**
	 * 获取请求命令原始字符串
	 * @return
	 */
	public String getCommand() {
		return command;
	}
	
}
