package com.googlecode.greysanatomy.console.command;

import com.googlecode.greysanatomy.console.FileValueConverter;
import com.googlecode.greysanatomy.console.InputCompleter;
import com.googlecode.greysanatomy.console.command.annotation.Cmd;
import com.googlecode.greysanatomy.console.command.annotation.IndexArg;
import com.googlecode.greysanatomy.console.command.annotation.NamedArg;
import com.googlecode.greysanatomy.util.GaReflectUtils;
import com.googlecode.greysanatomy.util.GaStringUtils;
import com.googlecode.greysanatomy.util.LogUtils;
import jline.console.ConsoleReader;
import jline.console.completer.*;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpecBuilder;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Commands {

    private final Logger logger = LogUtils.getLogger();
    private final Map<String, Class<?>> commands = new HashMap<String, Class<?>>();

    private Commands() {

        for (Class<?> clazz : GaReflectUtils.getClasses("com.googlecode.greysanatomy.console.command")) {

            if (!Command.class.isAssignableFrom(clazz)
                    || Modifier.isAbstract(clazz.getModifiers())) {
                continue;
            }

            if (clazz.isAnnotationPresent(Cmd.class)) {
                final Cmd cmd = clazz.getAnnotation(Cmd.class);
                commands.put(cmd.named(), clazz);
            }


        }

    }

    /**
     * 根据命令行所输入的内容构建一个命令
     *
     * @param line 命令行输入的一行命令
     * @return 解析出的命令
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public Command newCommand(String line) throws IllegalAccessException, InstantiationException {

        final String[] splitOfLine =
//                line.split("\\s+");
                GaStringUtils.splitForArgument(line);
        final String cmdName = splitOfLine[0];
        final Class<?> clazz = getInstance().commands.get(cmdName);
        if (null == clazz) {
            return null;
        }

        final Command command = (Command) clazz.newInstance();
        final OptionSet opt = getOptionParser(clazz).parse(splitOfLine);

        for (final Field field : clazz.getDeclaredFields()) {

            // 处理命名参数
            if (field.isAnnotationPresent(NamedArg.class)) {
                final NamedArg arg = field.getAnnotation(NamedArg.class);

                if (arg.hasValue()) {
                    if (opt.has(arg.named())) {
                        Object value = opt.valueOf(arg.named());

                        //如果是枚举类型，则根据枚举信息赋值
                        if (field.getType().isEnum()) {
                            final Enum<?>[] enums = (Enum[]) field.getType().getEnumConstants();
                            if (enums != null) {
                                for (Enum<?> e : enums) {
                                    if (e.name().equals(value)) {
                                        value = e;
                                        break;
                                    }
                                }
                            }
                        }
                        GaReflectUtils.set(field, value, command);
                    }
                }

                // 设置boolean类型,一般只有boolean类型hasValue才为false
                else {
                    GaReflectUtils.set(field, opt.has(arg.named()), command);
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
                    GaReflectUtils.set(field, opt.nonOptionArguments().get(index), command);
                }

            }

        }//for


        return command;
    }

    private static OptionParser getOptionParser(Class<?> clazz) {

        final StringBuilder sb = new StringBuilder();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(NamedArg.class)) {
                final NamedArg arg = field.getAnnotation(NamedArg.class);
                if (arg.hasValue()) {
                    sb.append(arg.named()).append(":");
                } else {
                    sb.append(arg.named());
                }
            }
        }

        final OptionParser parser
                = sb.length() == 0 ? new OptionParser() : new OptionParser(sb.toString());
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(NamedArg.class)) {
                final NamedArg arg = field.getAnnotation(NamedArg.class);
                if (arg.hasValue()) {
                    final OptionSpecBuilder osb = parser.accepts(arg.named(), arg.description());
                    osb.withOptionalArg()
                            .withValuesConvertedBy(new FileValueConverter())
                            .ofType(field.getType());
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


    private Collection<Completer> getCommandCompleterList() {
        final Collection<Completer> completerList = new ArrayList<Completer>();

        for (Map.Entry<String, Class<?>> entry : Commands.getInstance().listCommands().entrySet()) {
            ArgumentCompleter argCompleter = new ArgumentCompleter();
            completerList.add(argCompleter);
            argCompleter.getCompleters().add(new StringsCompleter(entry.getKey()));

            if (entry.getKey().equals("help")) {
                argCompleter.getCompleters().add(new StringsCompleter(Commands.getInstance().listCommands().keySet()));
            }

            for (Field field : GaReflectUtils.getFields(entry.getValue())) {
                if (field.isAnnotationPresent(NamedArg.class)) {
                    NamedArg arg = field.getAnnotation(NamedArg.class);
                    argCompleter.getCompleters().add(new StringsCompleter("-" + arg.named()));
                    if (File.class.isAssignableFrom(field.getType())) {
                        argCompleter.getCompleters().add(new FileNameCompleter());
                    }

//                    // boolean类型的已经通过自动识别的方式进行补充，不再需要用户主动填写
//                    else if (Boolean.class.isAssignableFrom(field.getType())
//                            || boolean.class.isAssignableFrom(field.getType())) {
//                        argCompleter.getCompleters().add(new StringsCompleter("true", "false"));
//                    }

                    else if (field.getType().isEnum()) {
                        Enum<?>[] enums = (Enum[]) field.getType().getEnumConstants();
                        String[] enumArgs = new String[enums.length];
                        for (int i = 0; i < enums.length; i++) {
                            enumArgs[i] = enums[i].name();
                        }
                        argCompleter.getCompleters().add(new StringsCompleter(enumArgs));
                    } else {
                        argCompleter.getCompleters().add(new InputCompleter());
                    }
                }
            }//for
            argCompleter.getCompleters().add(new NullCompleter());
        }

        return completerList;
    }

    /**
     * 注册提示信息
     *
     * @param console 控制台
     */
    public void regCompleter(ConsoleReader console) {
        console.addCompleter(new AggregateCompleter(getCommandCompleterList()));

    }

    private static final Commands instance = new Commands();

    public static synchronized Commands getInstance() {
        return instance;
    }

}
