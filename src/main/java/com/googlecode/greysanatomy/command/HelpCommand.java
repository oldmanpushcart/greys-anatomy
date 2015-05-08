package com.googlecode.greysanatomy.command;

import com.googlecode.greysanatomy.command.annotation.Cmd;
import com.googlecode.greysanatomy.command.annotation.IndexArg;
import com.googlecode.greysanatomy.command.annotation.NamedArg;
import com.googlecode.greysanatomy.command.view.KeyValueView;
import com.googlecode.greysanatomy.command.view.TableView;
import com.googlecode.greysanatomy.command.view.TableView.ColumnDefine;
import com.googlecode.greysanatomy.server.GaSession;

import java.lang.reflect.Field;
import java.util.*;

import static com.googlecode.greysanatomy.command.view.TableView.Align.LEFT;
import static com.googlecode.greysanatomy.command.view.TableView.Align.RIGHT;
import static com.googlecode.greysanatomy.util.GaStringUtils.isBlank;
import static java.lang.String.format;

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
public class HelpCommand extends Command {

    @IndexArg(index = 0, isRequired = false, name = "command-name", summary = "the name of command")
    private String cmd;

    @Override
    public Action getAction() {
        return new Action() {

            @Override
            public void action(final GaSession gaSession, final Info info, final Sender sender) throws Throwable {

                if (isBlank(cmd)
                        || !Commands.getInstance().listCommands().containsKey(cmd)) {
                    sender.send(true, mainHelp());
                } else {

                    final Class<?> clazz = Commands.getInstance().listCommands().get(cmd);
                    sender.send(true, commandHelp(clazz));

                }

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
        final KeyValueView view = new KeyValueView();
        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(NamedArg.class)) {
                final NamedArg namedArg = f.getAnnotation(NamedArg.class);
                final String named = "[" + namedArg.named() + (namedArg.hasValue() ? ":" : "") + "]";

                String description = namedArg.description();
                if (isBlank(namedArg.description2())) {
                    description += "\n" + namedArg.description2();
                }
                view.add(named, description);
            }
        }

        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(IndexArg.class)) {
                final IndexArg indexArg = f.getAnnotation(IndexArg.class);
                view.add(indexArg.name(), indexArg.description());
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
                .addRow("usage", drawUsage(clazz, cmd));

        boolean hasOptions = false;
        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(IndexArg.class)
                    || f.isAnnotationPresent(NamedArg.class)) {
                hasOptions = true;
                break;
            }
        }

        if (hasOptions) {
            view.addRow("options", drawOptions(clazz));
        }

        if (null != cmd.eg()) {
            view.addRow("example", drawEg(cmd));
        }

        return view.setDrawBorder(true).setPadding(1).draw();
    }

    /**
     * 输出主帮助菜单
     *
     * @return
     */
    private String mainHelp() {

        final KeyValueView kvView = new KeyValueView();

        final Map<String, Class<?>> commandMap = Commands.getInstance().listCommands();
        final List<Class<?>> classes = new ArrayList<Class<?>>(commandMap.values());
        Collections.sort(classes, new Comparator<Class<?>>() {

            @Override
            public int compare(Class<?> o1, Class<?> o2) {
                return new Integer(o1.getAnnotation(Cmd.class).sort()).compareTo(new Integer(o2.getAnnotation(Cmd.class).sort()));
            }

        });
        for (Class<?> clazz : classes) {

            if (clazz.isAnnotationPresent(Cmd.class)) {

                final Cmd cmd = clazz.getAnnotation(Cmd.class);
                kvView.add(cmd.named(), cmd.desc());

            }

        }

        return format("\nGreys usage:\n\n%s", kvView.draw());

    }

}
