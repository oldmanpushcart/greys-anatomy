package com.googlecode.greysanatomy.probe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProbeJobs {

	private static final Logger logger = LoggerFactory.getLogger("greysanatomy");
	
	/**
	 * 序列
	 */
	private static final AtomicInteger idseq = new AtomicInteger();

	/**
	 * 任务
	 * @author vlinux
	 *
	 */
	private static class Job {
		private int id;
		private boolean isAlive;
		private JobListener listener;
	}
	
	private static final Map<Integer,Job> jobs = new ConcurrentHashMap<Integer, Job>();
	
	
	/**
	 * 注册侦听器
	 * @param listener
	 */
	public static void register(int id, JobListener listener) {
		Job job = jobs.get(id);
		if( null != job ) {
			job.listener = listener;
			listener.create();
		}
	}
	
	/**
	 * 创建一个job
	 * @return
	 */
	public static int createJob() {
		final int id = idseq.incrementAndGet();
		Job job = new Job();
		job.id = id;
		job.isAlive = false;
		jobs.put(id, job);
		return id;
	}
	
	/**
	 * 激活一个job
	 * @param id
	 */
	public static void activeJob(int id) {
		Job job = jobs.get(id);
		if( null != job ) {
			job.isAlive = true;
		}
	}
	
	/**
	 * 判断job是否还可以继续工作
	 * @param id
	 * @return
	 */
	public static boolean isJobAlive(int id) {
		Job job = jobs.get(id);
		return null != job && job.isAlive;
	}
	
	/**
	 * 杀死一个job
	 * @param id
	 */
	public static void killJob(int id) {
		Job job = jobs.get(id);
		if( null != job ) {
			job.isAlive = false;
			try {
				job.listener.destroy();
			}catch(Throwable t) {
				logger.warn("destroy listener failed, jobId={}", id, t);
			}
		}
	}
	
	/**
	 * 返回存活的jobId
	 * @return
	 */
	public static List<Integer> listAliveJobIds() {
		final List<Integer> jobIds = new ArrayList<Integer>();
		for(Job job : jobs.values()) {
			if( job.isAlive ) {
				jobIds.add(job.id);
			}
		}
		return jobIds;
	}
	
	/**
	 * 返回当前的探测监听器列表
	 * @param id
	 * @return
	 */
	public static JobListener getJobListeners(int id) {
		if( jobs.containsKey(id) ) {
			return jobs.get(id).listener;
		} else {
			return null; 
		}
	}
	
	/**
	 * job是否实现了指定的listener
	 * @param id
	 * @param classListener
	 * @return
	 */
	public static boolean isListener(int id, Class<? extends JobListener> classListener) {
		
		final JobListener jobListener = getJobListeners(id);
		return null != jobListener 
				&& classListener.isAssignableFrom(jobListener.getClass());
		
	}
	
}
