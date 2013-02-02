package com.googlecode.greysanatomy.console.command;

import static java.lang.String.format;

import java.lang.reflect.Method;

import com.googlecode.greysanatomy.console.command.annotation.Arg;
import com.googlecode.greysanatomy.console.command.annotation.Cmd;
import com.googlecode.greysanatomy.util.GaDetailUtils;
import com.googlecode.greysanatomy.util.GaStringUtils;

/**
 * 展示方法信息
 * @author vlinux
 *
 */
@Cmd("search-method")
public class SearchMethodCommand extends Command {

	@Arg(name="class",isRequired=true)
	private String classRegex;
	
	@Arg(name="method",isRequired=true)
	private String methodRegex;
	
	@Arg(name="is-detail", isRequired=false)
	private boolean isDetail = false;
	
	@Override
	public Action getAction() {
		return new Action(){

			@Override
			public void action(Info info, Sender sender) throws Throwable {
				final StringBuilder message = new StringBuilder();
				int clzCnt = 0;
				int mthCnt = 0;
				for(Class<?> clazz :  info.getInst().getAllLoadedClasses()) {
					
					if( !clazz.getName().matches(classRegex) ) {
						continue;
					}
					
					boolean hasMethod = false;
					for( Method method : clazz.getDeclaredMethods() ) {
						
						if( method.getName().matches(methodRegex) ) {
							if( isDetail ) {
								message.append(GaDetailUtils.detail(method)).append("\n");
							} else {
								message.append(clazz.getName()).append("->").append(method.getName()).append("\n");
							}
							
							mthCnt++;
							hasMethod = true;
						}
						
					}//for
					
					if( hasMethod ) {
						clzCnt++;
					}
					
				}//for
				
				message.append(GaStringUtils.LINE);
				message.append(format("done. method result: match-class=%s; match-method=%s\n", clzCnt, mthCnt));
				
				sender.send(true, message.toString());
			}
			
		};
	}

}
