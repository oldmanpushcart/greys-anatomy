package com.googlecode.greysanatomy.console.command;

import static com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer.transform;
import static com.googlecode.greysanatomy.console.server.SessionJobsHolder.registJob;
import static com.googlecode.greysanatomy.probe.ProbeJobs.activeJob;

import java.lang.instrument.Instrumentation;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer.TransformResult;
import com.googlecode.greysanatomy.console.command.annotation.Arg;
import com.googlecode.greysanatomy.console.command.annotation.Cmd;
import com.googlecode.greysanatomy.console.command.parameter.WatchPointEnum;
import com.googlecode.greysanatomy.probe.Advice;
import com.googlecode.greysanatomy.probe.AdviceListenerAdapter;
import com.googlecode.greysanatomy.util.GaStringUtils;

@Cmd("watch")
public class WatchCommand extends Command {
	
	@Arg(name="class",isRequired=true)
	private String classRegex;
	
	@Arg(name="method",isRequired=true)
	private String methodRegex;
	
	@Arg(name="exp", isRequired=true)
	private String expression;
	
	@Arg(name="watch-point", isRequired=false)
	private WatchPointEnum watchPoint;

	@Override
	public Action getAction() {
		return new Action(){

			@Override
			public void action(Info info, final Sender sender) throws Throwable {
				ScriptEngine jsEngine = new ScriptEngineManager().getEngineByExtension("js");
				
				jsEngine.eval("function printWatch(p,o){o.send(false, " + expression + ");}");
				final Invocable invoke = (Invocable) jsEngine;
				
				final Instrumentation inst = info.getInst();
				final TransformResult result = transform(inst, classRegex, methodRegex, new AdviceListenerAdapter() {
					
					@Override
					public void onBefore(Advice p) {
						if(watchPoint == WatchPointEnum.before){
							try { invoke.invokeFunction("printWatch", p, sender); } catch (Exception e) { }
						}
					}

					@Override
					public void onFinish(Advice p) {
						if(watchPoint == WatchPointEnum.finish){
							try { invoke.invokeFunction("printWatch", p, sender); } catch (Exception e) { }
						}
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
