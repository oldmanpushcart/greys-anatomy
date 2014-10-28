package com.googlecode.greysanatomy.console.command;

import com.googlecode.greysanatomy.console.command.annotation.*;
import com.googlecode.greysanatomy.console.server.ConsoleServer;
import com.googlecode.greysanatomy.util.GaDetailUtils;
import com.googlecode.greysanatomy.util.GaStringUtils;
import com.googlecode.greysanatomy.util.SearchUtils;

import java.util.Set;

import static java.lang.String.format;

/**
 * 展示类信息
 *
 * @author vlinux
 */
@Cmd("search-class")
@RiscCmd(named="sc",sort = 0, desc="Search all have been loaded by the JVM class.")
public class SearchClassCommand extends Command {

    @Arg(name = "class")
    @RiscIndexArg(index = 0, name="class-regex", description = "regex match of classpath.classname")
    private String classRegex;

    @Arg(name = "is-super", isRequired = false)
    @RiscNamedArg(named = "s", description = "including class's parents")
    private boolean isSuper = false;

    @Arg(name = "is-detail", isRequired = false)
    @RiscNamedArg(named = "d", description = "show the detail of class")
    private boolean isDetail = false;

    @Override
    public Action getAction() {
        return new Action() {

            @Override
            public void action(final ConsoleServer consoleServer, final Info info, final Sender sender) throws Throwable {

                final StringBuilder message = new StringBuilder();
                final Set<Class<?>> matchs;
                if (isSuper) {

                    matchs = SearchUtils.searchClassBySupers(info.getInst(), SearchUtils.searchClassByClassRegex(info.getInst(), classRegex));
                } else {
                    matchs = SearchUtils.searchClassByClassRegex(info.getInst(), classRegex);
                }

                for (Class<?> clazz : matchs) {
                    if (isDetail) {
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
