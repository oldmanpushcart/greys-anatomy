package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.manager.ReflectManager;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.textui.ext.TClassInfo;
import com.github.ompc.greys.core.util.affect.RowAffect;
import com.github.ompc.greys.core.util.matcher.ClassMatcher;
import com.github.ompc.greys.core.util.matcher.PatternMatcher;

import java.lang.instrument.Instrumentation;
import java.util.Collection;

import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * 展示类信息
 *
 * @author oldmanpushcart@gmail.com
 */
@Cmd(name = "sc", sort = 0, summary = "Search all the classes loaded by JVM",
        eg = {
                "sc -E org\\.apache\\.commons\\.lang\\.StringUtils",
                "sc -d org.apache.commons.lang.StringUtils",
        })
public class SearchClassCommand implements Command {

    @IndexArg(index = 0, name = "class-pattern", summary = "Path and classname of Pattern Matching")
    private String classPattern;

    @NamedArg(name = "d", summary = "Display the details of class")
    private boolean isDetail = false;

    @NamedArg(name = "f", summary = "Display all the member variables")
    private boolean isField = false;

    @NamedArg(name = "E", summary = "Enable regular expression to match (wildcard matching by default)")
    private boolean isRegEx = false;

    private final ReflectManager reflectManager = ReflectManager.Factory.getInstance();

    @Override
    public Action getAction() {
        return new RowAction() {

            @Override
            public RowAffect action(Session session, Instrumentation inst, Printer printer) throws Throwable {

                final ClassMatcher classMatcher = new ClassMatcher(new PatternMatcher(isRegEx, classPattern));
                final Collection<Class<?>> matchedClassSet = reflectManager.searchClassWithSubClass(classMatcher);

                // 展示类详情
                if (isDetail) {

                    for (Class<?> clazz : matchedClassSet) {
                        printer.println(new TClassInfo(clazz, isField).rendering());
                    }

                }

                // 展示类该要列表
                else {

                    for (Class<?> clazz : matchedClassSet) {
                        printer.println(clazz.getName());
                    }

                }

                printer.print(EMPTY).finish();
                return new RowAffect(matchedClassSet.size());
            }

        };
    }

}
