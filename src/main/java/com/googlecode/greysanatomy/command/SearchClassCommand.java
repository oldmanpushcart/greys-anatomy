package com.googlecode.greysanatomy.command;

import com.googlecode.greysanatomy.command.annotation.Cmd;
import com.googlecode.greysanatomy.command.annotation.IndexArg;
import com.googlecode.greysanatomy.command.annotation.NamedArg;
import com.googlecode.greysanatomy.command.view.ClassInfoView;
import com.googlecode.greysanatomy.server.GaSession;

import java.util.Set;

import static com.googlecode.greysanatomy.util.GaStringUtils.LINE;
import static com.googlecode.greysanatomy.util.SearchUtils.searchClassByClassPatternMatching;
import static com.googlecode.greysanatomy.util.SearchUtils.searchClassBySupers;
import static java.lang.String.format;

/**
 * 展示类信息
 *
 * @author vlinux
 */
@Cmd(named = "sc", sort = 0, desc = "Search all have been loaded by the JVM class.",
        eg = {
                "sc -E org\\.apache\\.commons\\.lang\\.StringUtils",
                "sc -d org.apache.commons.lang.StringUtils",
                "sc -Sd *StringUtils"
        })
public class SearchClassCommand extends Command {

    @IndexArg(index = 0, name = "class-pattern", summary = "pattern matching of classpath.classname")
    private String classPattern;

    @NamedArg(named = "S", description = "including sub class")
    private boolean isSuper = false;

    @NamedArg(named = "d", description = "show the detail of class")
    private boolean isDetail = false;

    @NamedArg(named = "E", description = "enable the regex pattern matching")
    private boolean isRegEx = false;

    /**
     * 命令是否启用正则表达式匹配
     *
     * @return true启用正则表达式/false不启用
     */
    public boolean isRegEx() {
        return isRegEx;
    }

    @Override
    public Action getAction() {
        return new Action() {

            @Override
            public void action(final GaSession gaSession, final Info info, final Sender sender) throws Throwable {

                final StringBuilder message = new StringBuilder();
                final Set<Class<?>> matchedClassSet;
                if (isSuper) {

                    matchedClassSet = searchClassBySupers(
                            info.getInst(),
                            searchClassByClassPatternMatching(info.getInst(), classPattern, isRegEx()));
                } else {
                    matchedClassSet = searchClassByClassPatternMatching(info.getInst(), classPattern, isRegEx());
                }

                for (Class<?> clazz : matchedClassSet) {
                    if (isDetail) {
                        message.append(new ClassInfoView(clazz).draw()).append("\n");
                    } else {
                        message.append(clazz.getName()).append("\n");
                    }
                }

                message.append(LINE);
                message.append(format("result: matching-class=%s.", matchedClassSet.size()));
                sender.send(true, message.toString());
            }

        };
    }

}
