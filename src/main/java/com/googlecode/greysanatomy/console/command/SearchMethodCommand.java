package com.googlecode.greysanatomy.console.command;

import com.googlecode.greysanatomy.console.command.annotation.Cmd;
import com.googlecode.greysanatomy.console.command.annotation.IndexArg;
import com.googlecode.greysanatomy.console.command.annotation.NamedArg;
import com.googlecode.greysanatomy.console.server.ConsoleServer;
import com.googlecode.greysanatomy.util.GaDetailUtils;
import com.googlecode.greysanatomy.util.GaStringUtils;
import com.googlecode.greysanatomy.util.WildcardUtils;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;

/**
 * 展示方法信息
 *
 * @author vlinux
 */
@Cmd(named = "sm", sort = 1, desc = "Search all have been class method JVM loading.",
        eg = {
                "sm org.apache.commons.lang.StringUtils *",
                "sm -d org.apache.commons.lang.StringUtils *",
                "sm *String????s *",
                "sm org.apache.*StringUtils is*",
                "sm org.apache.*StringUtils",
        })
public class SearchMethodCommand extends Command {

    @IndexArg(index = 0, name = "class-wildcard", description = "wildcard match of classpath.classname")
    private String classWildcard;

    @IndexArg(index = 1, name = "method-wildcard", isRequired = false, description = "wildcard match of method name")
    private String methodWildcard = "*";

    @NamedArg(named = "d", description = "show the detail of method")
    private boolean isDetail = false;

    @Override
    public Action getAction() {
        return new Action() {

            @Override
            public void action(final ConsoleServer consoleServer, Info info, Sender sender) throws Throwable {
                final Set<String> uniqueLine = new HashSet<String>();
                final StringBuilder message = new StringBuilder();
                int clzCnt = 0;
                int mthCnt = 0;
                for (Class<?> clazz : info.getInst().getAllLoadedClasses()) {

//                    if (!clazz.getName().matches(classWildcard)) {
//                        continue;
//                    }

                    if (!WildcardUtils.match(clazz.getName(), classWildcard)) {
                        continue;
                    }

                    boolean hasMethod = false;
                    for (Method method : clazz.getDeclaredMethods()) {

                        if (/*method.getName().matches(methodWildcard)*/
                                WildcardUtils.match(method.getName(), methodWildcard)) {
                            if (isDetail) {
                                message.append(GaDetailUtils.detail(method)).append("\n");
                            } else {
                                /*
                                 * 过滤重复行
								 */
                                final String line = format("%s->%s\n", clazz.getName(), method.getName());
                                if (uniqueLine.contains(line)) {
                                    continue;
                                }
                                message.append(line);
                                uniqueLine.add(line);
                            }

                            mthCnt++;
                            hasMethod = true;
                        }

                    }//for

                    if (hasMethod) {
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
