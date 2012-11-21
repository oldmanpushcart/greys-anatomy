package com.googlecode.greysanatomy.console.network.serializer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 序列化工厂
 * @author vlinux
 *
 */
public class SerializerFactory {

	private static final Map<String, Serializer> serializers = new ConcurrentHashMap<String, Serializer>();
	
	/**
	 * Java序列化方式
	 */
	public static final String SERIALIZER_NAME_JAVA = "java";
	
	/**
	 * 注册序列化方式
	 * @param name
	 * @param serializer
	 */
	public static void register(String name, Serializer serializer) {
		serializers.put(name, serializer);
	}
	
	static {
		
		// 注册java序列化的方式
		register(SERIALIZER_NAME_JAVA, new JavaSerializer());
		
	}
	
	/**
	 * 获取序列化解析器
	 * @return
	 */
	public static Serializer getInstance() {
		return serializers.get(SERIALIZER_NAME_JAVA);
	}
	
}
