package com.github.ompc.greys.command;

import com.github.ompc.greys.GlobalOptions;
import com.github.ompc.greys.command.annotation.Cmd;
import com.github.ompc.greys.command.annotation.IndexArg;
import com.github.ompc.greys.command.annotation.NamedArg;
import com.github.ompc.greys.command.view.ClassInfoView;
import com.github.ompc.greys.server.Session;
import com.github.ompc.greys.util.Matcher;
import com.github.ompc.greys.util.Matcher.RegexMatcher;
import com.github.ompc.greys.util.Matcher.WildcardMatcher;
import com.github.ompc.greys.util.affect.RowAffect;

import java.lang.instrument.Instrumentation;
import java.util.Set;

import static com.github.ompc.greys.util.SearchUtils.searchClass;
import static com.github.ompc.greys.util.SearchUtils.searchSubClass;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * 展示类信息
 *
 * @author vlinux
 */
@Cmd(name = "sc", sort = 0, summary = "Search all have been loaded by the JVM class.",
        eg = {
                "sc -E org\\.apache\\.commons\\.lang\\.StringUtils",
                "sc -d org.apache.commons.lang.StringUtils",
                "sc -Sd *StringUtils"
        })
public class SearchClassCommand implements Command {

    @IndexArg(index = 0, name = "class-pattern", summary = "pattern matching of classpath.classname")
    private String classPattern;

    @NamedArg(name = "S", summary = "including sub class")
    private boolean isIncludeSub = GlobalOptions.isIncludeSubClass;

    @NamedArg(name = "d", summary = "show the detail of class")
    private boolean isDetail = false;

    @NamedArg(name = "f", summary = "show the declared fields of class")
    private boolean isField = false;

    @NamedArg(name = "E", summary = "enable the regex pattern matching")
    private boolean isRegEx = false;

    @Override
    public Action getAction() {
        return new RowAction() {

            @Override
            public RowAffect action(Session session, Instrumentation inst, Sender sender) throws Throwable {

                final Matcher classNameMatcher = isRegEx
                        ? new RegexMatcher(classPattern)
                        : new WildcardMatcher(classPattern);

                final Set<Class<?>> matchedClassSet = isIncludeSub
                        ? searchSubClass(inst, searchClass(inst, classNameMatcher))
                        : searchClass(inst, classNameMatcher);

                // 展示类详情
                if (isDetail) {

                    for (Class<?> clazz : matchedClassSet) {
                        sender.send(false, new ClassInfoView(clazz, isField).draw() + "\n");
                    }

                }

                // 展示类该要列表
                else {

                    for (Class<?> clazz : matchedClassSet) {
                        sender.send(false, clazz.getName() + "\n");
                    }

                }

                sender.send(true, EMPTY);
                return new RowAffect(matchedClassSet.size());
            }

        };
    }

}
