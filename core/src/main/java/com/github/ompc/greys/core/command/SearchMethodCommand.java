package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.util.Matcher;
import com.github.ompc.greys.core.util.Matcher.PatternMatcher;
import com.github.ompc.greys.core.util.affect.RowAffect;
import com.github.ompc.greys.core.view.LadderView;
import com.github.ompc.greys.core.view.MethodInfoView;
import com.github.ompc.greys.core.view.TableView;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;

import static com.github.ompc.greys.core.util.GaReflectUtils.getVisibleMethods;
import static com.github.ompc.greys.core.util.SearchUtils.searchClassWithSubClass;

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

    @IndexArg(index = 1, name = "method-pattern", summary = "Method of Pattern Matching")
    private String methodPattern;

    @NamedArg(name = "d", summary = "Display the details of method")
    private boolean isDetail = false;

    @NamedArg(name = "E", summary = "Enable regular expression to match (wildcard matching by default)")
    private boolean isRegEx = false;

    @Override
    public Action getAction() {

        final Matcher classNameMatcher = new PatternMatcher(isRegEx, classPattern);
        final Matcher methodNameMatcher = new PatternMatcher(isRegEx, methodPattern);

        return new RowAction() {

            @Override
            public RowAffect action(Session session, Instrumentation inst, Printer printer) throws Throwable {

                final RowAffect affect = new RowAffect();
                final LinkedHashSet<Class<?>> matchingClassSet = searchClassWithSubClass(inst, classNameMatcher);

                final TableView view = new TableView(new TableView.ColumnDefine[]{
                        new TableView.ColumnDefine(TableView.Align.LEFT),
                        new TableView.ColumnDefine(TableView.Align.LEFT),
                })
                        .addRow("DECLARED-CLASS", "VISIBLE-METHOD")
                        .hasBorder(true)
                        .padding(1);
                for (Class<?> clazz : matchingClassSet) {
                    drawSummary(view, clazz, methodNameMatcher, affect);
                }

                printer.print(view.draw()).finish();
                return affect;
            }

        };
    }


    /*
     * 绘制类方法摘要信息
     */
    private void drawSummary(final TableView view, final Class<?> clazz, final Matcher methodNameMatcher, final RowAffect affect) {

        final LadderView classLadderView = new LadderView();
        for (Map.Entry<Class<?>, LinkedHashSet<Method>> entry : getVisibleMethods(clazz).entrySet()) {

            final Class<?> clazzOfMethod = entry.getKey();
            classLadderView.addItem(clazzOfMethod.getName());
            final LinkedHashSet<Method> methodSet = entry.getValue();
            for (Method method : methodSet) {
                if (methodNameMatcher.matching(method.getName())) {
                    if (isDetail) {
                        view.addRow(classLadderView.draw(), new MethodInfoView(method).draw());
                    } else {
                        view.addRow(classLadderView.draw(), method.getName());
                    }
                    affect.rCnt(1);
                }
            }

        }

    }

}
