package com.googlecode.greysanatomy.console.command;

import static com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer.transform;
import static com.googlecode.greysanatomy.console.server.SessionJobsHolder.registJob;
import static com.googlecode.greysanatomy.probe.ProbeJobs.activeJob;

import java.lang.instrument.Instrumentation;

import com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer.TransformResult;
import com.googlecode.greysanatomy.console.command.annotation.Arg;
import com.googlecode.greysanatomy.console.command.annotation.Cmd;
import com.googlecode.greysanatomy.probe.Advice;
import com.googlecode.greysanatomy.probe.AdviceListenerAdapter;
import com.googlecode.greysanatomy.util.GaStringUtils;

/**
 * Jstack命令<br/>
 * 负责输出当前方法执行上下文
 * @author vlinux
 *
 */
@Cmd("jstack")
public class JstackCommand extends Command {

	@Arg(name="class",isRequired=true)
	private String classRegex;
	
	@Arg(name="method")
	private String methodRegex;
	
	@Override
	public Action getAction() {
		return new Action(){

			@Override
			public void action(Info info, final Sender sender) throws Throwable {
				
				final Instrumentation inst = info.getInst();
				final TransformResult result = transform(inst, classRegex, methodRegex, new AdviceListenerAdapter() {
					
					@Override
					public void onBefore(Advice p) {
						
						final String stackStr = GaStringUtils.getStack()+"\n";
						sender.send(false, stackStr);
						
					}
					
				},info);
				
				// 注册任务
				registJob(info.getSessionId(), result.getId());
				
				// 激活任务
				activeJob(result.getId());
				
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
