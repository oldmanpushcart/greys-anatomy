package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.textui.TLadder;
import com.github.ompc.greys.core.textui.TTable;
import com.github.ompc.greys.core.textui.ext.TMethodInfo;
import com.github.ompc.greys.core.util.Matcher;
import com.github.ompc.greys.core.util.Matcher.PatternMatcher;
import com.github.ompc.greys.core.util.affect.RowAffect;
import org.apache.commons.lang3.StringUtils;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;

import static com.github.ompc.greys.core.util.GaReflectUtils.getVisibleMethods;
import static com.github.ompc.greys.core.util.SearchUtils.searchClassWithSubClass;

/**
 * 展示方法信息
 *
 * @author oldmanpushcart@gmail.com
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

    @IndexArg(index = 1, isRequired = false, name = "method-pattern", summary = "Method of Pattern Matching")
    private String methodPattern;

    @NamedArg(name = "d", summary = "Display the details of method")
    private boolean isDetail = false;

    @NamedArg(name = "E", summary = "Enable regular expression to match (wildcard matching by default)")
    private boolean isRegEx = false;

    @Override
    public Action getAction() {

        final Matcher classNameMatcher = new PatternMatcher(isRegEx, classPattern);

        // 这里修复一个网友的咨询,如果methodPattern不填,是否可以默认为匹配为所有方法
        // 这个是我的一个疏忽,在老的版本中不填methodPattern确实greys会自动默认进行全方法匹配
        // 在某一个版本的优化中我随意去掉了这个功能,导致用户行为习惯受阻,非常抱歉
        final Matcher methodNameMatcher;
        if (StringUtils.isBlank(methodPattern)) {
            methodNameMatcher = new Matcher.TrueMatcher();
        } else {
            methodNameMatcher = new PatternMatcher(isRegEx, methodPattern);
        }


        return new RowAction() {

            @Override
            public RowAffect action(Session session, Instrumentation inst, Printer printer) throws Throwable {

                final RowAffect affect = new RowAffect();
                final LinkedHashSet<Class<?>> matchingClassSet = searchClassWithSubClass(inst, classNameMatcher);

                final TTable tTable = new TTable(new TTable.ColumnDefine[]{
                        new TTable.ColumnDefine(TTable.Align.LEFT),
                        new TTable.ColumnDefine(TTable.Align.LEFT),
                })
                        .addRow("DECLARED-CLASS", "VISIBLE-METHOD")
                        .padding(1);
                for (Class<?> clazz : matchingClassSet) {
                    drawSummary(tTable, clazz, methodNameMatcher, affect);
                }

                printer.print(tTable.rendering()).finish();
                return affect;
            }

        };
    }


    /*
     * 绘制类方法摘要信息
     */
    private void drawSummary(final TTable view, final Class<?> clazz, final Matcher methodNameMatcher, final RowAffect affect) {

        final TLadder classLadderView = new TLadder();
        for (Map.Entry<Class<?>, LinkedHashSet<Method>> entry : getVisibleMethods(clazz).entrySet()) {

            final Class<?> clazzOfMethod = entry.getKey();
            classLadderView.addItem(clazzOfMethod.getName());
            final LinkedHashSet<Method> methodSet = entry.getValue();
            for (Method method : methodSet) {
                if (methodNameMatcher.matching(method.getName())) {
                    if (isDetail) {
                        view.addRow(classLadderView.rendering(), new TMethodInfo(method).rendering());
                    } else {
                        view.addRow(classLadderView.rendering(), method.getName());
                    }
                    affect.rCnt(1);
                }
            }

        }

    }

}
