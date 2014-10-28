package com.googlecode.greysanatomy.console.command;

import com.googlecode.greysanatomy.console.command.annotation.RiscCmd;
import com.googlecode.greysanatomy.console.command.annotation.RiscIndexArg;
import com.googlecode.greysanatomy.console.command.annotation.RiscNamedArg;
import com.googlecode.greysanatomy.console.server.ConsoleServer;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 帮助明令<br/>
 * 这个类的代码丑得一B啊，我都不想看
 * Created by vlinux on 14/10/26.
 */
@RiscCmd(named = "help", sort = 10, desc = "List of the Greys command list.")
public class HelpCommand extends Command {

    @RiscIndexArg(index = 0, isRequired = false, name = "command-name", description = "the name of command")
    private String cmd;

    @Override
    public Action getAction() {
        return new Action() {

            @Override
            public void action(final ConsoleServer consoleServer, final Info info, final Sender sender) throws Throwable {
//                sender.send(true, GaStringUtils.getLogo());

                if (StringUtils.isBlank(cmd)
                        || !Commands.getInstance().listRiscCommands().containsKey(cmd)) {
                    sender.send(true, mainHelp());
                } else {

                    final Class<?> clazz = Commands.getInstance().listRiscCommands().get(cmd);
                    sender.send(true, commandHelp(clazz));

                }

            }

        };
    }

    private String commandHelp(Class<?> clazz) {
//        final Class<?> clazz= MonitorCommand.class;
        final RiscCmd cmd = clazz.getAnnotation(RiscCmd.class);
        final StringBuilder sb = new StringBuilder("\nUseage of ")
                .append(cmd.named())
                .append(" :\n\n");

        sb.append("\t");

        final StringBuilder sbOp = new StringBuilder();
        for (Field f : clazz.getDeclaredFields()) {

            if (f.isAnnotationPresent(RiscNamedArg.class)) {
                final RiscNamedArg namedArg = f.getAnnotation(RiscNamedArg.class);
                sbOp.append(namedArg.named());
                if (namedArg.hasValue()) {
                    sbOp.append(":");
                }
            }

        }
        if (sbOp.length() > 0) {
            sb.append("-[").append(sbOp).append("]").append(" ");
        }

        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(RiscIndexArg.class)) {
                final RiscIndexArg indexArg = f.getAnnotation(RiscIndexArg.class);
                sb.append(indexArg.name()).append(" ");
            }
        }

        sb.append("\n");
        sb.append("\t").append(cmd.desc()).append("\n\n");

        sb.append("\nOptions :\n\n");

        int maxCol = 10;
        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(RiscIndexArg.class)) {
                final RiscIndexArg indexArg = f.getAnnotation(RiscIndexArg.class);
                maxCol = Math.max(indexArg.name().length(), maxCol);
            }
            if (f.isAnnotationPresent(RiscNamedArg.class)) {
                final RiscNamedArg namedArg = f.getAnnotation(RiscNamedArg.class);
                maxCol = Math.max(namedArg.named().length(), maxCol);
            }

        }


        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(RiscNamedArg.class)) {
                int len = 0;
                final RiscNamedArg namedArg = f.getAnnotation(RiscNamedArg.class);
                final String named = "[" + namedArg.named() + (namedArg.hasValue() ? ":" : "") + "]";
                final int diff = Math.max(maxCol, named.length()) - named.length();
                for (int i = 0; i < diff + 2; i++) {
                    sb.append(" ");
                }
                sb.append(named).append(" : ");
                sb.append(namedArg.description());
                sb.append("\n");

            }
        }

        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(RiscIndexArg.class)) {
                final RiscIndexArg indexArg = f.getAnnotation(RiscIndexArg.class);
                final int diff = Math.max(maxCol, indexArg.name().length()) - indexArg.name().length();
                for (int i = 0; i < diff + 2; i++) {
                    sb.append(" ");
                }
                sb.append(indexArg.name()).append(" : ");
                sb.append(indexArg.description());
                sb.append("\n");

                int len = diff + 2 + indexArg.name().length()+3;
                if( !StringUtils.isBlank(indexArg.description2()) ) {

                    for( String split : StringUtils.split(indexArg.description2(),"\n") ) {
                        for( int j=0;j<len;j++ ) {
                            sb.append(" ");
                        }
                        sb.append(split).append("\n");
                    }

                }

            }
        }

        return sb.toString();
    }

    /**
     * 输出主帮助菜单
     *
     * @return
     */
    private String mainHelp() {

        final Map<String, Class<?>> commandMap = Commands.getInstance().listRiscCommands();
        int maxCommandColLen = 9;
        final StringBuilder sb = new StringBuilder();

        sb.append("\nUsage for Greys : \n\n");

        for (Class<?> clazz : commandMap.values()) {
            if (clazz.isAnnotationPresent(RiscCmd.class)) {
                final RiscCmd cmd = clazz.getAnnotation(RiscCmd.class);
                final String name = cmd.named();
                maxCommandColLen = Math.max(maxCommandColLen, name.length());
            }
        }


        final List<Class<?>> classes = new ArrayList<Class<?>>(commandMap.values());
        Collections.sort(classes, new Comparator<Class<?>>() {

            @Override
            public int compare(Class<?> o1, Class<?> o2) {
                return new Integer(o1.getAnnotation(RiscCmd.class).sort()).compareTo(new Integer(o2.getAnnotation(RiscCmd.class).sort()));
            }

        });
        for (Class<?> clazz : classes) {

            if (clazz.isAnnotationPresent(RiscCmd.class)) {

                final RiscCmd cmd = clazz.getAnnotation(RiscCmd.class);
                final String name = cmd.named();
                final String desc = cmd.desc();
                final int diff = maxCommandColLen - name.length();
                for (int i = 0; i < diff; i++) {
                    sb.append(" ");
                }
                sb.append(name).append(" : ").append(desc).append("\n");

            }

        }

        return sb.toString();

    }

}
