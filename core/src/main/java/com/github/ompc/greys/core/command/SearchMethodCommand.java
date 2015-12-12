package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.manager.ReflectManager;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.textui.TLadder;
import com.github.ompc.greys.core.textui.TTable;
import com.github.ompc.greys.core.textui.ext.TGaMethodInfo;
import com.github.ompc.greys.core.util.GaMethod;
import com.github.ompc.greys.core.util.affect.RowAffect;
import com.github.ompc.greys.core.util.matcher.ClassMatcher;
import com.github.ompc.greys.core.util.matcher.GaMethodMatcher;
import com.github.ompc.greys.core.util.matcher.PatternMatcher;
import com.github.ompc.greys.core.util.matcher.TrueMatcher;
import org.apache.commons.lang3.StringUtils;

import java.lang.instrument.Instrumentation;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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

    private final ReflectManager reflectManager = ReflectManager.Factory.getInstance();

    @Override
    public Action getAction() {

        return new RowAction() {

            @Override
            public RowAffect action(Session session, Instrumentation inst, Printer printer) throws Throwable {

                final RowAffect affect = new RowAffect();

                final ClassMatcher classMatcher = new ClassMatcher(new PatternMatcher(isRegEx, classPattern));
                final TTable tTable = new TTable(new TTable.ColumnDefine[]{
                        new TTable.ColumnDefine(TTable.Align.LEFT),
                        new TTable.ColumnDefine(TTable.Align.LEFT),
                })
                        .addRow("DECLARED-CLASS", "VISIBLE-METHOD")
                        .padding(1);
                for (Class<?> clazz : reflectManager.searchClassWithSubClass(classMatcher)) {
                    renderingMethodSummary(tTable, clazz, affect);
                }

                printer.print(tTable.rendering()).finish();
                return affect;
            }

        };
    }


    /*
     * 构造方法名匹配
     * 这里修复一个网友的咨询,如果methodPattern不填,是否可以默认为匹配为所有方法
     * 这个是我的一个疏忽,在老的版本中不填methodPattern确实greys会自动默认进行全方法匹配
     * 在某一个版本的优化中我随意去掉了这个功能,导致用户行为习惯受阻,非常抱歉
     */
    private GaMethodMatcher toGaMethodMatcher() {
        return new GaMethodMatcher(
                StringUtils.isBlank(methodPattern)
                        ? new TrueMatcher<String>()
                        : new PatternMatcher(isRegEx, methodPattern)
        );
    }


    /*
     * 渲染类方法摘要信息
     */
    private void renderingMethodSummary(final TTable view, final Class<?> clazz, final RowAffect affect) {

        final TLadder classLadderView = new TLadder();
        final GaMethodMatcher gaMethodMatcher = toGaMethodMatcher();
        final Collection<GaMethod> gaMethods = reflectManager.searchClassGaMethods(clazz, gaMethodMatcher);
        final Set<Class<?>> uniqueSet = new HashSet<Class<?>>();

        classLadderView.addItem(clazz.getName());
        uniqueSet.add(clazz);

        for (GaMethod gaMethod : gaMethods) {

            final Class<?> declaringClass = gaMethod.getDeclaringClass();
            if (!uniqueSet.contains(declaringClass)) {
                classLadderView.addItem(declaringClass.getName());
                uniqueSet.add(declaringClass);
            }

            if (isDetail) {
                view.addRow(classLadderView.rendering(), new TGaMethodInfo(gaMethod).rendering());
            } else {
                view.addRow(classLadderView.rendering(), gaMethod.getName());
            }
            affect.rCnt(1);

        }

    }

}
