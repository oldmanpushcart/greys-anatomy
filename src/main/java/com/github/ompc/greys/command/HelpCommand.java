package com.github.ompc.greys.command;


import com.github.ompc.greys.command.annotation.Cmd;
import com.github.ompc.greys.command.annotation.IndexArg;
import com.github.ompc.greys.command.annotation.NamedArg;
import com.github.ompc.greys.command.view.KVView;
import com.github.ompc.greys.command.view.TableView;
import com.github.ompc.greys.command.view.TableView.ColumnDefine;
import com.github.ompc.greys.server.Session;
import com.github.ompc.greys.util.RowAffect;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.util.*;

import static com.github.ompc.greys.command.view.TableView.Align.LEFT;
import static com.github.ompc.greys.command.view.TableView.Align.RIGHT;
import static com.github.ompc.greys.util.StringUtil.isBlank;


/**
 * 帮助明令<br/>
 * 这个类的代码丑得一B啊，我都不想看
 * <p/>
 * Created by vlinux on 14/10/26.
 */
@Cmd(named = "help", sort = 12, desc = "List of the Greys command list.",
        eg = {
                "help",
                "help sc",
                "help sm",
                "help watch"
        })
public class HelpCommand implements Command {

    @IndexArg(index = 0, isRequired = false, name = "command-name", summary = "the name of command")
    private String cmd;

    @Override
    public Action getAction() {
        return new RowAction() {

            @Override
            public RowAffect action(Session session, Instrumentation inst, Sender sender) throws Throwable {
                if (isBlank(cmd)
                        || !Commands.getInstance().listCommands().containsKey(cmd)) {
                    sender.send(true, mainHelp());
                } else {
                    final Class<?> clazz = Commands.getInstance().listCommands().get(cmd);
                    sender.send(true, commandHelp(clazz));
                }

                return new RowAffect(1);
            }
        };

    }

    private String drawUsage(final Class<?> clazz, final Cmd cmd) {

        final StringBuilder usageSB = new StringBuilder();
        final StringBuilder sbOp = new StringBuilder();
        for (Field f : clazz.getDeclaredFields()) {

            if (f.isAnnotationPresent(NamedArg.class)) {
                final NamedArg namedArg = f.getAnnotation(NamedArg.class);
                sbOp.append(namedArg.named());
                if (namedArg.hasValue()) {
                    sbOp.append(":");
                }
            }

        }
        if (sbOp.length() > 0) {
            usageSB.append("-[").append(sbOp).append("]").append(" ");
        }

        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(IndexArg.class)) {
                final IndexArg indexArg = f.getAnnotation(IndexArg.class);
                usageSB.append(indexArg.name()).append(" ");
            }
        }

        usageSB.append("\n").append(cmd.desc());

        return usageSB.toString();
    }

    private String drawOptions(Class<?> clazz) {
        final KVView view = new KVView();
        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(NamedArg.class)) {
                final NamedArg namedArg = f.getAnnotation(NamedArg.class);
                final String named = "[" + namedArg.named() + (namedArg.hasValue() ? ":" : "") + "]";

                String description = namedArg.summary();
                if (isBlank(namedArg.description())) {
                    description += "\n" + namedArg.description();
                }
                view.add(named, description);
            }
        }

        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(IndexArg.class)) {
                final IndexArg indexArg = f.getAnnotation(IndexArg.class);
                String description = indexArg.summary();
                if (isBlank(indexArg.description())) {
                    description += "\n" + indexArg.description();
                }
                view.add(indexArg.name(), description);
            }
        }

        return view.draw();
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
        final TableView view = new TableView(new ColumnDefine[]{
                new ColumnDefine(RIGHT),
                new ColumnDefine(LEFT)
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
            view.addRow("OPTIONS", drawOptions(clazz));
        }

        if (null != cmd.eg()) {
            view.addRow("EXAMPLE", drawEg(cmd));
        }

        return view.border(true).padding(1).draw();
    }


    /*
     * 输出主帮助菜单
     */
    private String mainHelp() {

        final TableView view = new TableView(new ColumnDefine[]{
                new ColumnDefine(RIGHT),
                new ColumnDefine(LEFT)
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
                view.addRow(cmd.named(), cmd.desc());
            }

        }

        return view.border(true).padding(1).draw();
    }

}
