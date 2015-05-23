package com.github.ompc.greys.command;

import com.github.ompc.greys.command.annotation.Cmd;
import com.github.ompc.greys.command.annotation.IndexArg;
import com.github.ompc.greys.command.annotation.NamedArg;
import com.github.ompc.greys.command.view.ClassInfoView;
import com.github.ompc.greys.command.view.TableView;
import com.github.ompc.greys.command.view.TableView.ColumnDefine;
import com.github.ompc.greys.server.Session;
import com.github.ompc.greys.util.Matcher;
import com.github.ompc.greys.util.Matcher.RegexMatcher;
import com.github.ompc.greys.util.Matcher.WildcardMatcher;
import com.github.ompc.greys.command.affect.RowAffect;

import java.lang.instrument.Instrumentation;
import java.util.Set;

import static com.github.ompc.greys.command.view.TableView.Align.LEFT;
import static com.github.ompc.greys.util.SearchUtil.searchClass;
import static com.github.ompc.greys.util.SearchUtil.searchSubClass;
import static com.github.ompc.greys.util.StringUtil.*;

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
public class SearchClassCommand implements Command {

    @IndexArg(index = 0, name = "class-pattern", summary = "pattern matching of classpath.classname")
    private String classPattern;

    @NamedArg(named = "S", summary = "including sub class")
    private boolean isIncludeSub = false;

    @NamedArg(named = "d", summary = "show the detail of class")
    private boolean isDetail = false;

    @NamedArg(named = "E", summary = "enable the regex pattern matching")
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
                        sender.send(false, new ClassInfoView(clazz).draw() + "\n");
                    }

                }

                // 展示类该要列表
                else {

                    final TableView view = new TableView(new ColumnDefine[]{
                            new ColumnDefine(50, false, LEFT),
                            new ColumnDefine(50, false, LEFT),
                            new ColumnDefine(LEFT),
                            new ColumnDefine(LEFT)
                    })
                            .addRow(
                                    "CLASS-LOADER",
                                    "CLASS-NAME",
                                    "TYPE",
                                    "MODIFIER"
                            );

                    for (Class<?> clazz : matchedClassSet) {
                        view.addRow(
                                newString(clazz.getClassLoader()),
                                clazz.getName(),
                                getType(clazz),
                                rowToCol(tranModifier(clazz.getModifiers()), ",")
                        );
                    }

                    sender.send(false, view.border(true).padding(1).draw());

                }

                sender.send(true, EMPTY);
                return new RowAffect(matchedClassSet.size());
            }

        };
    }


    /*
     * 获取对象类型
     */
    private String getType(Class<?> clazz) {

        if (clazz.isAnnotation()) {
            return "Annotation";
        } else if (clazz.isEnum()) {
            return "Enum";
        } else if (clazz.isInterface()) {
            return "Interface";
        } else {
            return "Class";
        }

    }

}
