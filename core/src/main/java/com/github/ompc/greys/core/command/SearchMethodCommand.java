package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.view.MethodInfoView;
import com.github.ompc.greys.core.util.SearchUtils;
import com.github.ompc.greys.core.util.affect.RowAffect;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.util.Matcher;
import com.github.ompc.greys.core.util.Matcher.RegexMatcher;
import com.github.ompc.greys.core.util.Matcher.WildcardMatcher;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * 展示方法信息
 *
 * @author vlinux
 */
@Cmd(name = "sm", sort = 1, summary = "Search the method of classes loaded by JVM",
        eg = {
                "sm -Ed org\\.apache\\.commons\\.lang\\.StringUtils .*",
                "sm org.apache.commons.????.StringUtils *",
                "sm -d org.apache.commons.lang.StringUtils",
                "sm *String????s *"
        })
public class SearchMethodCommand implements Command {

    @IndexArg(index = 0, name = "class-pattern", summary = "Path and classname of Pattern Matching")
    private String classPattern;

    @IndexArg(index = 1, name = "method-pattern", isRequired = false, summary = "Method of Pattern Matching")
    private String methodPattern;

    @NamedArg(name = "d", summary = "Display the details of method")
    private boolean isDetail = false;

    @NamedArg(name = "E", summary = "Enable regular expression to match (wildcard matching by default)")
    private boolean isRegEx = false;

    @Override
    public Action getAction() {

        final Matcher classNameMatcher = isRegEx
                ? new RegexMatcher(classPattern)
                : new WildcardMatcher(classPattern);

        // auto fix default methodPattern
        if (isBlank(methodPattern)) {
            methodPattern = isRegEx ? ".*" : "*";
        }

        final Matcher methodNameMatcher = isRegEx
                ? new RegexMatcher(methodPattern)
                : new WildcardMatcher(methodPattern);

        return new RowAction() {

            @Override
            public RowAffect action(Session session, Instrumentation inst, Printer printer) throws Throwable {

                final Set<String> uniqueLine = new HashSet<String>();
                final StringBuilder message = new StringBuilder();

                final RowAffect affect = new RowAffect();
                final Set<Class<?>> matchingClassSet = SearchUtils.searchClass(inst, classNameMatcher);

                for (Class<?> clazz : matchingClassSet) {
                    for (Method method : clazz.getDeclaredMethods()) {

                        if (methodNameMatcher.matching(method.getName())) {
                            if (isDetail) {
                                message.append(new MethodInfoView(method).draw()).append("\n");
                            } else {
                                /*
                                 * 过滤重复行
								 */
                                final String line = format("%s->%s%n", clazz.getName(), method.getName());
                                if (uniqueLine.contains(line)) {
                                    continue;
                                }
                                message.append(line);
                                uniqueLine.add(line);
                            }

                            affect.rCnt(1);
                        }

                    }//for
                }//for

                printer.print(message.toString()).finish();
                return affect;
            }

        };
    }
    
}
