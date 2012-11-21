package com.googlecode.greysanatomy.console.network.serializer;

import java.io.Serializable;

/**
 * 序列化
 * @author vlinux
 *
 */
public interface Serializer {

	/**
	 * 序列化
	 * @param serializable
	 * @return
	 * @throws SerializationException
	 */
	<T extends Serializable> byte[] encode(T serializable) throws SerializationException;
	
	/**
	 * 反序列化
	 * @param bytes
	 * @return
	 * @throws SerializationException
	 */
	<T extends Serializable> T decode(byte[] bytes) throws SerializationException;
	
}
