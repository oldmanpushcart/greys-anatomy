package com.googlecode.greysanatomy.console.command;

import static java.lang.String.format;

import java.util.Set;

import com.googlecode.greysanatomy.console.command.annotation.Arg;
import com.googlecode.greysanatomy.console.command.annotation.Cmd;
import com.googlecode.greysanatomy.util.GaDetailUtils;
import com.googlecode.greysanatomy.util.GaStringUtils;
import com.googlecode.greysanatomy.util.SearchUtils;

/**
 * 展示类信息
 * @author vlinux
 *
 */
@Cmd("search-class")
public class SearchClassCommand extends Command {

	@Arg(name="class")
	private String classRegex;
	
	@Arg(name="is-super", isRequired=false)
	private boolean isSuper = false;
	
	@Arg(name="is-detail", isRequired=false)
	private boolean isDetail = false;
	
	@Override
	public Action getAction() {
		return new Action(){

			@Override
			public void action(final Info info, final Sender sender) throws Throwable {
				
				final StringBuilder message = new StringBuilder();
				final Set<Class<?>> matchs;
				if( isSuper ) {
					
					matchs = SearchUtils.searchClassBySupers(info.getInst(), SearchUtils.searchClassByClassRegex(info.getInst(), classRegex));
				} else {
					matchs = SearchUtils.searchClassByClassRegex(info.getInst(), classRegex);
				}
				
				for( Class<?> clazz : matchs ) {
					if( isDetail ) {
						message.append(GaDetailUtils.detail(clazz)).append("\n");
					} else {
						message.append(clazz.getName()).append("\n");
					}
				}
				
				message.append(GaStringUtils.LINE);
				message.append(format("done. classes result: match-class=%s;\n", matchs.size()));
				sender.send(true, message.toString());
			}
			
		};
	}
	
}
