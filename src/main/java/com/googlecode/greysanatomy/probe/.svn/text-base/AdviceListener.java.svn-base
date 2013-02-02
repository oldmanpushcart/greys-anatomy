package com.googlecode.greysanatomy.probe;

/**
 * 探测点监听器接口
 * @author vlinux
 *
 */
public interface AdviceListener extends JobListener {

	/**
	 * 调用开始
	 * @param advice
	 */
	void onBefore(Advice advice);
	
	/**
	 * 调用成功
	 * @param advice
	 */
	void onSuccess(Advice advice);
	
	/**
	 * 调用抛出异常
	 * @param advice
	 */
	void onException(Advice advice);
	
	/**
	 * 调用结束(成功+抛出异常)
	 * @param advice
	 */
	void onFinish(Advice advice);
	
}
