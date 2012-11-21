package com.googlecode.greysanatomy.console.network.coder;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 命令追踪信息
 * @author vlinux
 *
 */
public class CmdTracer implements Serializable{

	private static final long serialVersionUID = -7415043715453515498L;

	/*
	 * 追踪序列(client-server唯一)
	 */
	private static transient final AtomicLong seq = new AtomicLong();
	
	/*
	 * 本次追踪序列
	 */
	private final long id;
	
	/**
	 * 用于生成ReqCmd
	 */
	protected CmdTracer() {
		id = seq.incrementAndGet();
	}
	
	/**
	 * 用于生成RespCmd
	 * @param id
	 */
	protected CmdTracer(long id) {
		this.id = id;
	}

	
	/**
	 * 获取本次追踪序列
	 * @return
	 */
	public long getId() {
		return id;
	}
	
}
