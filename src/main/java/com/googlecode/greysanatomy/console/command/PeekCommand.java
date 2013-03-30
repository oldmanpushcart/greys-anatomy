package com.googlecode.greysanatomy.console.command;

import static java.lang.String.format;

import java.lang.reflect.Field;
import java.util.Set;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.googlecode.greysanatomy.console.command.annotation.Arg;
import com.googlecode.greysanatomy.console.command.annotation.Cmd;
import com.googlecode.greysanatomy.util.GaStringUtils;
import com.googlecode.greysanatomy.util.SearchUtils;

@Cmd("peek")
public class PeekCommand extends Command {

	@Arg(name="class",isRequired=true)
	private String classRegex;
	
	@Arg(name="field",isRequired=true)
	private String fieldName;
	
	@Arg(name="exp", isRequired=false)
	private String expression = "$F($this,null)";
	
	@Override
	public Action getAction() {
		return new Action(){

			@Override
			public void action(final Info info, final Sender sender) throws Throwable {
				ScriptEngine jsEngine = new ScriptEngineManager().getEngineByExtension("js");
				jsEngine.eval("var $F=com.googlecode.greysanatomy.util.GaReflectUtils.getFieldValueByFieldName;");
				jsEngine.eval("function peek($this,m){try{m.append(" + expression + ")}catch(e){m.append(e.message);}}");
				final Invocable invoke = (Invocable) jsEngine;
				
				final StringBuilder message = new StringBuilder();
				final Set<Class<?>> matchs = SearchUtils.searchClassByClassRegex(info.getInst(), classRegex);
				
				message.append(GaStringUtils.LINE);
				message.append(format("done. classes result: match-class=%s;\n", matchs.size()));
				for( Class<?> clazz : matchs ) {
					message.append(clazz.getName());
					Field field = null;
					boolean isFieldAccessable = false;
					try{
						field = clazz.getDeclaredField(fieldName);
						isFieldAccessable = field.isAccessible();
						field.setAccessible(true);
						message.append(".").append(fieldName).append(" peaked value").append("=["); 
						invoke.invokeFunction("peek", field.get(null), message);
						message.append("]\n");
					}catch(Exception e){
						message.append(" getField exception:")
							.append(e.getClass().getName()).append(":")
							.append(e.getMessage()).append("\n");
					}finally{
						if(field != null){
							field.setAccessible(isFieldAccessable);
						}
					}
				}
				
				sender.send(true, message.toString());
			}
		};
	}
	
}
