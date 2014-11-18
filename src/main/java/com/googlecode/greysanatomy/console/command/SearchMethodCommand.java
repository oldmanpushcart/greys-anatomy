package com.googlecode.greysanatomy.console.command;

<<<<<<< HEAD
import com.googlecode.greysanatomy.console.command.annotation.*;
=======
import com.googlecode.greysanatomy.console.command.annotation.RiscCmd;
import com.googlecode.greysanatomy.console.command.annotation.RiscIndexArg;
import com.googlecode.greysanatomy.console.command.annotation.RiscNamedArg;
>>>>>>> pr/8
import com.googlecode.greysanatomy.console.server.ConsoleServer;
import com.googlecode.greysanatomy.util.GaDetailUtils;
import com.googlecode.greysanatomy.util.GaStringUtils;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;

/**
 * 展示方法信息
 *
 * @author vlinux
 */
<<<<<<< HEAD
@Cmd("search-method")
@RiscCmd(named = "sm", sort = 1, desc = "Search all have been class method JVM loading.")
public class SearchMethodCommand extends Command {

    @Arg(name = "class", isRequired = true)
    @RiscIndexArg(index = 0, name = "class-regex", description = "regex match of classpath.classname")
    private String classRegex;

    @Arg(name = "method", isRequired = true)
    @RiscIndexArg(index = 1, name = "method-regex", description = "regex match of methodname")
    private String methodRegex;

    @Arg(name = "is-detail", isRequired = false)
=======
@RiscCmd(named = "sm", sort = 1, desc = "Search all have been class method JVM loading.",
        eg = {
                "sm org\\.apache\\.commons\\.lang\\.StringUtils .*",
                "sm -d org\\.apache\\.commons\\.lang\\.StringUtils .*",
        })
public class SearchMethodCommand extends Command {

    @RiscIndexArg(index = 0, name = "class-regex", description = "regex match of classpath.classname")
    private String classRegex;

    @RiscIndexArg(index = 1, name = "method-regex", description = "regex match of methodname")
    private String methodRegex;

>>>>>>> pr/8
    @RiscNamedArg(named = "d", description = "show the detail of method")
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

                    if (!clazz.getName().matches(classRegex)) {
                        continue;
                    }

                    boolean hasMethod = false;
                    for (Method method : clazz.getDeclaredMethods()) {

                        if (method.getName().matches(methodRegex)) {
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
