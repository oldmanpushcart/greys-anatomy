package com.googlecode.greysanatomy;

import static com.googlecode.greysanatomy.util.GaReflectUtils.*;
import static com.googlecode.greysanatomy.util.GaStringUtils.*;

import java.lang.reflect.Field;

import org.apache.commons.lang.StringUtils;

/**
 * 配置类
 * @author vlinux
 *
 */
public class Configer {

	/*
	 * 控制台连接端口
	 */
	private int consolePort = 3658;
	
	/*
	 * 对方java进程号
	 */
	private int javaPid;
	
	/*
	 * 连接超时时间(ms)
	 */
	private long connectTimeout = 6000;
	
	/*
	 * 控制台提示符
	 */
	private String consolePrompt = "ga?>";
	
	public Configer() {
		//
	}
	
	/**
	 * 将Configer对象转换为字符串
	 */
	public String toString() {
		final StringBuilder strSB = new StringBuilder();
		for(Field field : getFileds(Configer.class)) {
			try {
				strSB.append(field.getName()).append("=").append(encode(newString(getFieldValueByField(this, field)))).append(";");
			}catch(Throwable t) {
				//
			}
		}//for
		return strSB.toString();
	}
	
	/**
	 * 将toString的内容转换为Configer对象
	 * @param toString
	 * @return
	 */
	public static Configer toConfiger(String toString) {
		final Configer configer = new Configer();
		final String[] pvs = StringUtils.split(toString,";");
		for( String pv : pvs ) {
			try {
				final String[] strs = StringUtils.split(pv,"=");
				final String p = strs[0];
				final String v = decode(strs[1]);
				final Field field = getField(Configer.class, p);
				set(field, valueOf(field.getType(), v), configer);
			}catch(Throwable t) {
				//
			}
		}
		return configer;
	}

	/**
	 * 获取控制台端口
	 * @return
	 */
	public int getConsolePort() {
		return consolePort;
	}

	/**
	 * 设置控制台端口
	 * @param consolePort
	 */
	public void setConsolePort(int consolePort) {
		this.consolePort = consolePort;
	}

	/**
	 * 获取目标java进程号
	 * @return
	 */
	public int getJavaPid() {
		return javaPid;
	}

	/**
	 * 设置目标java进程号
	 * @param javaPid
	 */
	public void setJavaPid(int javaPid) {
		this.javaPid = javaPid;
	}

	/**
	 * 获取连接超时时间
	 * @return
	 */
	public long getConnectTimeout() {
		return connectTimeout;
	}

	/**
	 * 设置连接超时时间
	 * @param connectTimeout
	 */
	public void setConnectTimeout(long connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	/**
	 * 获取控制台提示符
	 * @return
	 */
	public String getConsolePrompt() {
		return consolePrompt;
	}

	/**
	 * 设置控制台提示符
	 * @param consolePrompt
	 */
	public void setConsolePrompt(String consolePrompt) {
		this.consolePrompt = consolePrompt;
	}
	
}
