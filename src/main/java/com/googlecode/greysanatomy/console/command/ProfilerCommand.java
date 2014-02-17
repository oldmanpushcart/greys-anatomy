package com.googlecode.greysanatomy.console.command;

import static com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer.transform;
import static com.googlecode.greysanatomy.console.server.SessionJobsHolder.registJob;
import static com.googlecode.greysanatomy.probe.ProbeJobs.activeJob;

import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer.TransformResult;
import com.googlecode.greysanatomy.clocker.Clocker;
import com.googlecode.greysanatomy.console.command.annotation.Arg;
import com.googlecode.greysanatomy.console.command.annotation.Cmd;
import com.googlecode.greysanatomy.probe.Advice;
import com.googlecode.greysanatomy.probe.AdviceListenerAdapter;
import com.googlecode.greysanatomy.util.GaStringUtils;
import com.googlecode.greysanatomy.util.ProfilerUtils;

@Cmd("profiler")
public class ProfilerCommand extends Command {

	@Arg(name="class")
	private String classRegex;
	
	@Arg(name="method")
	private String methodRegex;
	
	@Arg(name="probe-class")
	private String probeClassRegex;
	
	@Arg(name="probe-method")
	private String probeMethodRegex;
	
	@Arg(name="cost")
	private long cost;
	
	@Override
	public Action getAction() {
		return new Action(){

			private final ThreadLocal<Boolean> isEntered = new ThreadLocal<Boolean>();
			private final ThreadLocal<Integer> deep = new ThreadLocal<Integer>();
			private final ThreadLocal<Long> beginTimestamp = new ThreadLocal<Long>();
			private final Map<String, Boolean> cmCache = new ConcurrentHashMap<String, Boolean>();
			
			@Override
			public void action(Info info, final Sender sender) throws Throwable {
				
				final Instrumentation inst = info.getInst();
				final AdviceListenerAdapter advice = new AdviceListenerAdapter() {
					
					@Override
					public void onBefore(Advice p) {
						init();
						if( !isEntered(p) ) {
							return;
						}
						if( 0 == deep.get() ) {
							beginTimestamp.set(Clocker.current().getCurrentTimeMillis());
							isEntered.set(true);
							ProfilerUtils.start("");
						}
						ProfilerUtils.enter();
						deep.set(deep.get()+1);
					}

					@Override
					public void onFinish(Advice p) {
						if( !isEntered.get() ) {
							return;
						}
						deep.set(deep.get()-1);
						ProfilerUtils.release();
						if( 0 == deep.get() ) {
							final long cost = Clocker.current().getCurrentTimeMillis() - beginTimestamp.get();
							final String dump = ProfilerUtils.dump();
							if( cost >= ProfilerCommand.this.cost ) {
								final StringBuilder dumpSB = new StringBuilder()
									.append("Thread Info:").append(Thread.currentThread().getName()).append("\n")
									.append(dump).append("\n\n");
								sender.send(false, dumpSB.toString());
							}
							isEntered.set(false);
						}
					}
					
					private void init() {
						if( null == deep.get() ) {
							deep.set(0);
						}
						if( null == isEntered.get() ) {
							isEntered.set(false);
						}
					}
					
					private boolean isEntered(Advice p) {
						if( isEntered.get() ) {
							return true;
						}
						final String cmKey = new StringBuilder()
							.append(p.getTarget().getTargetClass().getName())
							.append("#")
							.append(p.getTarget().getTargetBehavior().getName())
							.toString();
						
						if( cmCache.containsKey(cmKey) ) {
							return cmCache.get(cmKey);
						} else {
							final boolean isProbe = p.getTarget().getTargetClass().getName().matches(probeClassRegex) 
									&& p.getTarget().getTargetBehavior().getName().matches(probeMethodRegex);
							cmCache.put(cmKey, isProbe);
							return isProbe;
						}
					}
					
				};
				final TransformResult result = transform(inst, classRegex, methodRegex, advice,info);
				final TransformResult resultForProbe = transform(inst, probeClassRegex, probeMethodRegex, advice,info);
				
				// 注册任务
				registJob(info.getSessionId(), result.getId());
				registJob(info.getSessionId(), resultForProbe.getId());
			
				// 激活任务
				activeJob(result.getId());
				activeJob(resultForProbe.getId());
				
				final StringBuilder message = new StringBuilder();
				message.append(GaStringUtils.LINE);
				message.append(String.format("done. probe:c-Cnt=%s,m-Cnt=%s\n", 
						result.getModifiedClasses().size(),
						result.getModifiedBehaviors().size()));
				sender.send(false, message.toString());
			}
			
		};
	}

}
