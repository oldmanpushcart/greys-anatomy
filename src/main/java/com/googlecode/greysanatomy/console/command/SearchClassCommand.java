package com.googlecode.greysanatomy.console.command;

import com.googlecode.greysanatomy.console.command.annotation.Cmd;
import com.googlecode.greysanatomy.console.command.annotation.IndexArg;
import com.googlecode.greysanatomy.console.command.annotation.NamedArg;
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
@Cmd(named = "sc", sort = 0, desc = "Search all have been loaded by the JVM class.",
        eg = {
                "sc org\\.apache\\.commons\\.lang\\.StringUtils",
                "sc -s org\\.apache\\.commons\\.lang\\.StringUtils",
                "sc -d org\\.apache\\.commons\\.lang\\.StringUtils",
                "sc -sd .*StringUtils"
        })
public class SearchClassCommand extends Command {

    @IndexArg(index = 0, name = "class-regex", description = "regex match of classpath.classname")
    private String classRegex;

    @NamedArg(named = "s", description = "including class's parents")
    private boolean isSuper = false;

    @NamedArg(named = "d", description = "show the detail of class")
    private boolean isDetail = false;

    @Override
    public Action getAction() {
        return new Action() {

            @Override
            public void action(final ConsoleServer consoleServer, final Info info, final Sender sender) throws Throwable {

                final StringBuilder message = new StringBuilder();
                final Set<Class<?>> matchedClassSet;
                if (isSuper) {

                    matchedClassSet = SearchUtils.searchClassBySupers(info.getInst(), SearchUtils.searchClassByClassRegex(info.getInst(), classRegex));
                } else {
                    matchedClassSet = SearchUtils.searchClassByClassRegex(info.getInst(), classRegex);
                }

                for (Class<?> clazz : matchedClassSet) {
                    if (isDetail) {
                        message.append(GaDetailUtils.detail(clazz)).append("\n");
                    } else {
                        message.append(clazz.getName()).append("\n");
                    }
                }

                message.append(GaStringUtils.LINE);
                message.append(format("done. classes result: match-class=%s;\n", matchedClassSet.size()));
                sender.send(true, message.toString());
            }

        };
    }

}
