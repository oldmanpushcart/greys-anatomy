package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.exception.CommandException;
import com.github.ompc.greys.core.exception.CommandInitializationException;
import com.github.ompc.greys.core.exception.CommandNotFoundException;
import com.github.ompc.greys.core.util.GaReflectUtils;
import com.github.ompc.greys.core.util.GaStringUtils;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpecBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Commands {

    private final Map<String, Class<?>> commands = new HashMap<String, Class<?>>();

    private Commands() {

        for (Class<?> clazz : GaReflectUtils.getClasses(Commands.class.getClassLoader(), "com.github.ompc.greys.core.command")) {

            if (!Command.class.isAssignableFrom(clazz)
                    || Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }

            if (clazz.isAnnotationPresent(Cmd.class)) {
                final Cmd cmd = clazz.getAnnotation(Cmd.class);
                commands.put(cmd.name(), clazz);
            }

        }

    }

    /**
     * 根据命令行所输入的内容构建一个命令
     *
     * @param line 命令行输入的一行命令
     * @return 解析出的命令
     * @throws CommandException 命令失败(不存在/初始化失败/参数校验等)
     */
    public Command newCommand(String line) throws CommandException {

        final String[] splitOfLine = GaStringUtils.splitForArgument(line);
        final String cmdName = splitOfLine[0];
        final Class<?> clazz = getInstance().commands.get(cmdName);
        if (null == clazz) {
            throw new CommandNotFoundException(cmdName);
        }

        final Command command;
        try {
            command = (Command) clazz.newInstance();
        } catch (Throwable t) {
            throw new CommandInitializationException(cmdName, t);
        }


        try {
            final OptionSet opt = getOptionParser(clazz).parse(splitOfLine);

            for (final Field field : clazz.getDeclaredFields()) {

                // 处理命名参数
                if (field.isAnnotationPresent(NamedArg.class)) {
                    final NamedArg arg = field.getAnnotation(NamedArg.class);

                    if (arg.hasValue()) {
                        if (opt.has(arg.name())) {

                            final boolean isCollection = field.getType().isAssignableFrom(Collection.class);
                            final Object value;

                            // 处理一参多值的情况
                            if (isCollection) {
                                value = opt.valuesOf(arg.name());
                            }

                            // 处理一参单值的情况
                            else {

                                // 待定的返回值,稍后可能用枚举值修正
                                Object valueOfUndetermined = opt.valueOf(arg.name());

                                //如果是枚举类型，则根据枚举信息赋值
                                if (field.getType().isEnum()) {
                                    final Enum<?>[] enums = (Enum[]) field.getType().getEnumConstants();
                                    if (enums != null) {
                                        for (Enum<?> e : enums) {
                                            if (e.name().equals(valueOfUndetermined)) {
                                                valueOfUndetermined = e;
                                                break;
                                            }
                                        }
                                    }
                                }

                                value = valueOfUndetermined;
                            }

                            try {
                                GaReflectUtils.set(field, value, command);
                            } catch (IllegalAccessException e) {
                                throw new CommandInitializationException(cmdName, e);
                            }
                        }
                    }

                    // 设置boolean类型,一般只有boolean类型hasValue才为false
                    else {
                        try {
                            GaReflectUtils.set(field, opt.has(arg.name()), command);
                        } catch (IllegalAccessException e) {
                            throw new CommandInitializationException(cmdName, e);
                        }
                    }


                }

                // 处理顺序参数
                else if (field.isAnnotationPresent(IndexArg.class)) {
                    final IndexArg arg = field.getAnnotation(IndexArg.class);
                    final int index = arg.index() + 1;
                    if (arg.isRequired()
                            && opt.nonOptionArguments().size() <= index) {
                        throw new IllegalArgumentException(arg.name() + " argument was missing.");
                    }

                    if (opt.nonOptionArguments().size() > index) {
                        try {
                            GaReflectUtils.set(field, opt.nonOptionArguments().get(index), command);
                        } catch (IllegalAccessException e) {
                            throw new CommandInitializationException(cmdName, e);
                        }
                    }

                }

            }//for
        } catch (Throwable t) {
            throw new CommandException(cmdName, t);
        }

        return command;
    }

    private static OptionParser getOptionParser(Class<?> clazz) {

        final OptionParser parser = new OptionParser();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(NamedArg.class)) {
                final NamedArg arg = field.getAnnotation(NamedArg.class);
                final OptionSpecBuilder osb = parser.accepts(arg.name(), arg.summary());
                if (arg.hasValue()) {
                    final boolean isCollection = field.getType().isAssignableFrom(Collection.class);

                    if( isCollection ) {
                        osb.withOptionalArg();
                    } else {
                        osb.withOptionalArg().ofType(field.getType());
                    }
                }
            }
        }

        return parser;
    }

    /**
     * 列出所有精简命令
     *
     * @return 返回当前版本所支持的精简命令集合
     */
    public Map<String, Class<?>> listCommands() {
        return new HashMap<String, Class<?>>(commands);
    }

    private static final Commands instance = new Commands();

    public static synchronized Commands getInstance() {
        return instance;
    }

}
