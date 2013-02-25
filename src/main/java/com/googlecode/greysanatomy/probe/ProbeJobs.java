package com.googlecode.greysanatomy.probe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.greysanatomy.console.command.JavaScriptCommand.JLS;

public class ProbeJobs {

	private static final Logger logger = LoggerFactory.getLogger("greysanatomy");
	
	/**
	 * ����
	 * @author vlinux
	 *
	 */
	private static class Job {
		private String id;
		private boolean isAlive;
		private JobListener listener;
	}
	
	private static final Map<String,Job> jobs = new ConcurrentHashMap<String, Job>();
	
	
	/**
	 * ע��������
	 * @param listener
	 */
	public static void register(String id, JobListener listener) {
		Job job = jobs.get(id);
		if( null != job ) {
			job.listener = listener;
			listener.create();
		}
	}
	
	/**
	 * ����һ��job
	 * @return
	 */
	public static String createJob() {
		final String id = UUID.randomUUID().toString();
		Job job = new Job();
		job.id = id;
		job.isAlive = false;
		jobs.put(id, job);
		return id;
	}
	
	/**
	 * ����һ��job
	 * @param id
	 */
	public static void activeJob(String id) {
		Job job = jobs.get(id);
		if( null != job ) {
			job.isAlive = true;
		}
	}
	
	/**
	 * �ж�job�Ƿ񻹿��Լ�������
	 * @param id
	 * @return
	 */
	public static boolean isJobAlive(String id) {
		Job job = jobs.get(id);
		return null != job && job.isAlive;
	}
	
	/**
	 * ɱ��һ��job
	 * @param id
	 */
	public static void killJob(String id) {
		Job job = jobs.get(id);
		if( null != job ) {
			job.isAlive = false;
			try {
				job.listener.destroy();
			}catch(Throwable t) {
				logger.warn("destroy listener failed, jobId={}", id, t);
			}
			JLS.removeJob(id);
		}
	}
	
	/**
	 * ���ش���jobId
	 * @return
	 */
	public static List<String> listAliveJobIds() {
		final List<String> jobIds = new ArrayList<String>();
		for(Job job : jobs.values()) {
			if( job.isAlive ) {
				jobIds.add(job.id);
			}
		}
		return jobIds;
	}
	
	/**
	 * ���ص�ǰ��̽��������б�
	 * @param id
	 * @return
	 */
	public static JobListener getJobListeners(String id) {
		if( jobs.containsKey(id) ) {
			return jobs.get(id).listener;
		} else {
			return null; 
		}
	}
	
	/**
	 * job�Ƿ�ʵ����ָ����listener
	 * @param id
	 * @param classListener
	 * @return
	 */
	public static boolean isListener(String id, Class<? extends JobListener> classListener) {
		
		final JobListener jobListener = getJobListeners(id);
		return null != jobListener 
				&& classListener.isAssignableFrom(jobListener.getClass());
		
	}
	
}
