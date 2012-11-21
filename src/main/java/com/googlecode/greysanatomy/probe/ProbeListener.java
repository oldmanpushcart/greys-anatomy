package com.googlecode.greysanatomy.probe;

/**
 * 探测点监听器接口
 * @author vlinux
 *
 */
public interface ProbeListener {

	/**
	 * 监听器创建
	 */
	void create();
	
	/**
	 * 监听器销毁
	 */
	void destroy();
	
	/**
	 * 调用开始
	 * @param p
	 */
	void onBefore(Probe p);
	
	/**
	 * 调用成功
	 * @param p
	 */
	void onSuccess(Probe p);
	
	/**
	 * 调用抛出异常
	 * @param p
	 */
	void onException(Probe p);
	
	/**
	 * 调用结束(成功+抛出异常)
	 * @param p
	 */
	void onFinish(Probe p);
	
}
