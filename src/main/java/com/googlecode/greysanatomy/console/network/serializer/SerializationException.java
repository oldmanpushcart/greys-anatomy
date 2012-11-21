package com.googlecode.greysanatomy.console.network.serializer;

/**
 * 序列化异常<br/>
 * 需要用来屏蔽各种序列化方式的不同而产生的各种不同异常
 * @author vlinux
 *
 */
public class SerializationException extends Exception {

	private static final long serialVersionUID = 2892644844139339521L;

	public SerializationException(Throwable t) {
		super(t);
	}
	
}
