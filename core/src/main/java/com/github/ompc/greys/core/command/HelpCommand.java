package com.github.ompc.greys.core.command;


import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.textui.TTable;
import com.github.ompc.greys.core.util.affect.RowAffect;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;


/**
 * 帮助明令<br/>
 * 这个类的代码丑得一B啊，我都不想看
 * <p/>
 * Created by oldmanpushcart@gmail.com on 14/10/26.
 */
@Cmd(name = "help", sort = 12, summary = "Display Greys Help",
        eg = {
                "help",
                "help sc",
                "help sm",
                "help watch"
        })
public class HelpCommand implements Command {

    @IndexArg(index = 0, isRequired = false, name = "command-name", summary = "Command name")
    private String cmd;

    @Override
    public Action getAction() {
        return new RowAction() {

            @Override
            public RowAffect action(Session session, Instrumentation inst, Printer printer) throws Throwable {
                if (isBlank(cmd)
                        || !Commands.getInstance().listCommands().containsKey(cmd)) {
                    printer.print(mainHelp()).finish();
                } else {
                    final Class<?> clazz = Commands.getInstance().listCommands().get(cmd);
                    printer.print(commandHelp(clazz)).finish();
                }

                return new RowAffect(1);
            }
        };

    }

    private String drawUsage(final Class<?> clazz, final Cmd cmd) {

        final StringBuilder usageSB = new StringBuilder();
        final StringBuilder sbOp = new StringBuilder();
        final StringBuilder sbLongOp = new StringBuilder();
        for (Field f : clazz.getDeclaredFields()) {

            if (f.isAnnotationPresent(NamedArg.class)) {
                final NamedArg namedArg = f.getAnnotation(NamedArg.class);
                if (namedArg.name().length() == 1) {
                    sbOp.append(namedArg.name());
                    if (namedArg.hasValue()) {
                        sbOp.append(":");
                    }
                } else {
                    sbLongOp.append(namedArg.name());
                    if (namedArg.hasValue()) {
                        sbLongOp.append(":");
                    }
                }

            }

        }
        if (sbOp.length() > 0) {
            usageSB.append("-[").append(sbOp).append("]").append(" ");
        }

        if (sbLongOp.length() > 0) {
            usageSB.append("--[").append(sbLongOp).append("]").append(" ");
        }

        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(IndexArg.class)) {
                final IndexArg indexArg = f.getAnnotation(IndexArg.class);
                usageSB.append(indexArg.name()).append(" ");
            }
        }

        if (usageSB.length() > 0) {
            usageSB.append("\n");
        }
        usageSB.append(cmd.summary());

        return usageSB.toString();
    }

    private String drawOptions(Class<?> clazz) {
        final TTable tTable = new TTable(new TTable.ColumnDefine[]{
                new TTable.ColumnDefine(15, false, TTable.Align.RIGHT),
                new TTable.ColumnDefine(60, false, TTable.Align.LEFT)
        });

        tTable.getBorder().remove(TTable.Border.BORDER_OUTER);
        tTable.padding(1);

//        final TKv tKv = new TKv(new TTable.ColumnDefine(20, false, TTable.Align.RIGHT), new TTable.ColumnDefine(50, false, TTable.Align.LEFT));
        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(NamedArg.class)) {
                final NamedArg namedArg = f.getAnnotation(NamedArg.class);
                final String named = "[" + namedArg.name() + (namedArg.hasValue() ? ":" : "") + "]";

                String description = namedArg.summary();
                if (isNotBlank(namedArg.description())) {
                    description += "\n\n" + namedArg.description();
                }
                tTable.addRow(named, description);
                // tKv.add(named, description);
            }
        }

        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(IndexArg.class)) {
                final IndexArg indexArg = f.getAnnotation(IndexArg.class);
                String description = indexArg.summary();
                if (isNotBlank(indexArg.description())) {
                    description += "\n\n" + indexArg.description();
                }
                tTable.addRow(indexArg.name(), description);
                // tKv.add(indexArg.name(), description);
            }
        }

        return tTable.rendering();
    }

    private String drawEg(Cmd cmd) {
        final StringBuilder egSB = new StringBuilder();
        for (String eg : cmd.eg()) {
            egSB.append(eg).append("\n");
        }
        return egSB.toString();
    }

    private String commandHelp(Class<?> clazz) {

        final Cmd cmd = clazz.getAnnotation(Cmd.class);
        final TTable tTable = new TTable(new TTable.ColumnDefine[]{
                new TTable.ColumnDefine(TTable.Align.RIGHT),
                new TTable.ColumnDefine(80, false, TTable.Align.LEFT)
        })
                .addRow("USAGE", drawUsage(clazz, cmd));

        boolean hasOptions = false;
        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(IndexArg.class)
                    || f.isAnnotationPresent(NamedArg.class)) {
                hasOptions = true;
                break;
            }
        }

        if (hasOptions) {
            tTable.addRow("OPTIONS", drawOptions(clazz));
        }

        if (null != cmd.eg()) {
            tTable.addRow("EXAMPLE", drawEg(cmd));
        }

        return tTable.padding(1).rendering();
    }


    /*
     * 输出主帮助菜单
     */
    private String mainHelp() {

        final TTable tTable = new TTable(new TTable.ColumnDefine[]{
                new TTable.ColumnDefine(TTable.Align.RIGHT),
                new TTable.ColumnDefine(80, false, TTable.Align.LEFT)
        });

        final Map<String, Class<?>> commandMap = Commands.getInstance().listCommands();
        final List<Class<?>> classes = new ArrayList<Class<?>>(commandMap.values());
        Collections.sort(classes, new Comparator<Class<?>>() {

            @Override
            public int compare(Class<?> o1, Class<?> o2) {
                final Integer o1s = o1.getAnnotation(Cmd.class).sort();
                final Integer o2s = o2.getAnnotation(Cmd.class).sort();
                return o1s.compareTo(o2s);
            }

        });
        for (Class<?> clazz : classes) {

            if (clazz.isAnnotationPresent(Cmd.class)) {
                final Cmd cmd = clazz.getAnnotation(Cmd.class);
                if (!cmd.isHacking()) {
                    tTable.addRow(cmd.name(), cmd.summary());
                }
            }

        }

        return tTable.padding(1).rendering();
    }

}
