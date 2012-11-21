package com.googlecode.greysanatomy.console.network.coder;

/**
 * 应答命令
 * @author vlinux
 *
 */
public class RespCmd extends CmdTracer {

	private static final long serialVersionUID = -6448961415701231840L;
	
	/*
	 * 应答信息
	 */
	private final String message;
	
	/*
	 * 交互是否结束
	 */
	private final boolean isFinish;

	/**
	 * 构造应答命令
	 * @param id
	 * @param isFinish
	 * @param message
	 */
	public RespCmd(long id, boolean isFinish, String message) {
		super(id);
		this.isFinish = isFinish;
		this.message = message;
	}

	/**
	 * 获取应答信息
	 * @return
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * 交互是否结束
	 * @return
	 */
	public boolean isFinish() {
		return isFinish;
	}
	
}
