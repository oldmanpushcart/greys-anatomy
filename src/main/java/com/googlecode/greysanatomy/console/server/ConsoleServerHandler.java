package com.googlecode.greysanatomy.console.server;

import static com.googlecode.greysanatomy.console.server.SessionJobsHolder.heartBeatSession;
import static com.googlecode.greysanatomy.console.server.SessionJobsHolder.registSession;
import static com.googlecode.greysanatomy.console.server.SessionJobsHolder.unRegistJob;
import static com.googlecode.greysanatomy.probe.ProbeJobs.createJob;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.instrument.Instrumentation;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.greysanatomy.console.command.Command;
import com.googlecode.greysanatomy.console.command.Command.Action;
import com.googlecode.greysanatomy.console.command.Command.Info;
import com.googlecode.greysanatomy.console.command.Command.Sender;
import com.googlecode.greysanatomy.console.command.Commands;
import com.googlecode.greysanatomy.console.rmi.RespResult;
import com.googlecode.greysanatomy.console.rmi.req.ReqCmd;
import com.googlecode.greysanatomy.console.rmi.req.ReqGetResult;
import com.googlecode.greysanatomy.console.rmi.req.ReqHeart;
import com.googlecode.greysanatomy.console.rmi.req.ReqKillJob;
import com.googlecode.greysanatomy.util.JvmUtils;
import com.googlecode.greysanatomy.util.JvmUtils.ShutdownHook;

/**
 * 控制台服务端处理器
 * @author vlinux
 *
 */
public class ConsoleServerHandler {

	private static final Logger logger = LoggerFactory.getLogger("greysanatomy");
	
	private final Instrumentation inst;
	private final ExecutorService workers;
	
	public ConsoleServerHandler(Instrumentation inst) {
		this.inst = inst;
		this.workers = Executors.newCachedThreadPool(new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r, "ga-console-server-workers");
				t.setDaemon(true);
				return t;
			}

		});
		
		JvmUtils.registShutdownHook("ga-console-server", new ShutdownHook(){

			@Override
			public void shutdown() throws Throwable {
				if( null != workers ) {
					workers.shutdown();
				}
			}
			
		});
		
	}
	
	public RespResult postCmd(final ReqCmd cmd) {
		final RespResult respResult = new RespResult();
		respResult.setSessionId(cmd.getGaSessionId());
		respResult.setJobId(createJob());
		workers.execute(new Runnable(){

			@Override
			public void run() {
				// 普通命令请求
				final Info info = new Info(inst, respResult.getSessionId(), respResult.getJobId());
				final Sender sender = new Sender(){

					@Override
					public void send(boolean isF, String message) {
						write(respResult.getSessionId(), respResult.getJobId(), isF, message);
					}
				};
				
				try {
					final Command command = Commands.getInstance().newCommand(cmd.getCommand());
					// 命令不存在
					if( null == command ) {
						write(respResult.getSessionId(), respResult.getJobId(), true, "command not found!");
						return;
					}
					final Action action = command.getAction();
					action.action(info, sender);
				}catch(Throwable t) {
					// 执行命令失败
					logger.warn("do action failed.", t);
					write(respResult.getSessionId(), respResult.getJobId(), true, "do action failed. cause:"+t.getMessage());
					return;
				}
			}
			
		});
		return respResult;
	}

	public long register() {
		return registSession();
	}

	public RespResult getCmdExecuteResult(ReqGetResult req) {
		RespResult respResult = new RespResult();
		if(!heartBeatSession(req.getGaSessionId())){
			respResult.setMessage("session Timeout.please reload!");
			respResult.setFinish(true);
			return respResult;
		}
		read(req.getJobId(), req.getPos(), respResult);
		respResult.setFinish(isFinish(respResult.getMessage()));
		return respResult;
	}

	public void killJob(ReqKillJob req) {
		unRegistJob(req.getGaSessionId(), req.getJobId());
	}

	public boolean sessionHeartBeat(ReqHeart req) {
		return heartBeatSession(req.getGaSessionId());
	}
	
	/**
	 * 执行结果输出文件路径
	 */
	private final String executeResultDir = System.getProperty("java.io.tmpdir") + "/greysdata/";
	
	private final String executeResultFileExtensions = ".ga";
	
	private final String endMark = ""+(char)29;
	
	/**
	 * 写结果
	 * @param gaSessionId
	 * @param jobId
	 * @param isF
	 * @param message
	 */
	private void write(long gaSessionId, String jobId, boolean isF, String message) {
		if(isF){
			message += endMark;
		}
		
		if(StringUtils.isEmpty(message)){
			return;
		}
		
		RandomAccessFile rf;
		
		try {
			new File(executeResultDir).mkdir();
			rf = new RandomAccessFile(getExecuteFilePath(jobId), "rw");
			rf.seek(rf.length());
			rf.write(message.getBytes());
			rf.close();
		} catch (IOException e) {
			logger.warn("jobFile write error!",e);
			return ;
		}  
	}
	
	/**
	 * 读job的结果
	 * @param gaSessionId
	 * @param jobId
	 * @param pos
	 * @param message
	 * @return
	 */
	private void read(String jobId, int pos, RespResult respResult) {
		RandomAccessFile rf;
		StringBuilder sb = new StringBuilder();
		int newPos = pos;
		try {
			rf = new RandomAccessFile(getExecuteFilePath(jobId), "r");
			rf.seek(pos);
			byte[] buffer = new byte[10000]; 
	        int len=0; 
			while ((len=rf.read(buffer))!=-1) { 
				newPos += len;
				sb.append(new String(buffer,0,len)); 
	        } 
			rf.close();
		} catch (IOException e) {
			logger.warn("jobFile read error!");
			return ;
		}  
		respResult.setPos(newPos);
		respResult.setMessage(sb.toString());
	}
	
	private String getExecuteFilePath(String jobId){
		return executeResultDir + jobId + executeResultFileExtensions;
	}
	
	private boolean isFinish(String message){
		return !StringUtils.isEmpty(message) ? message.endsWith(endMark) : false;
	}
}
