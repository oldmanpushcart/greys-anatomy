package com.github.ompc.greys.client.command;

import com.github.ompc.greys.client.util.GaClientStringUtils;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpecBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.github.ompc.greys.client.util.GaClientStringUtils.splitForLine;

/**
 * 命令解析器
 * <p>
 * 用于将一个命令字符串解析成可以被后端执行的URL
 * </p>
 */
public class CommandParser {

    private final Logger logger = LoggerFactory.getLogger(CommandParser.class);
    private final Map<String, Command> commands;

    /**
     * 构造命令解析器
     *
     * @throws CommandInitializationException 命令解析器初始化失败
     */
    public CommandParser() throws CommandInitializationException {
        try {
            this.commands = new MappingSaxParser().parse();
        } catch (Throwable cause) {
            throw new CommandInitializationException(cause);
        }
    }

    /**
     * 用于封装HTTP参数的Map
     */
    private class ParamMap extends LinkedHashMap<String, List<String>> {

        /**
         * 追加PUT
         *
         * @param key   HTTP-NAME
         * @param value HTTP-VALUE
         * @return HTTP-VALUE[]
         */
        List<String> appendPut(String key, String value) {
            final List<String> values;
            if (containsKey(key)) {
                values = get(key);
            } else {
                put(key, values = new ArrayList<String>());
            }
            values.add(value);
            return values;
        }
    }

    /**
     * 获取命令对象集合
     *
     * @return 命令对象集合
     */
    public Map<String, Command> getCommandMap() {
        return commands;
    }

    /**
     * 转换成为HTTP的QueryString格式
     *
     * @param line 命令行
     * @return line所对应的QueryString
     * @throws CommandNotFoundException 命令不存在
     * @throws IllegalArgumentException 命令参数非法
     * @throws CommandParserException   命令解析失败
     */
    public String parse(final String line)
            throws CommandNotFoundException, IllegalArgumentException, CommandParserException {

        final String[] lineSegmentArray = splitForLine(line);
        final String commandName = lineSegmentArray[0];
        final Command command = foundCommand(commandName);
        final ParamMap httpParameters = new ParamMap();

        final OptionSet opt;
        try {
            opt = getOptionParser(command).parse(lineSegmentArray);
        } catch (OptionException oe) {
            throw new CommandParserException(commandName, oe);
        }

        // 处理命名参数
        for (final Param param : command.getNamedParams().values()) {

            if (param.isRequired()
                    && !opt.has(param.getName())) {
                throw new IllegalArgumentException(param.getName() + " argument was missing.");
            }

            if (opt.has(param.getName())) {
                if (!param.isHasValue()) {
                    httpParameters.appendPut(param.getHttpName(), param.getHttpValue());
                } else {
                    httpParameters.appendPut(param.getHttpName(), opt.valueOf(param.getName()).toString());
                }
            }
        }

        // 处理顺序参数
        int index = 0;
        for (final Param param : command.getIndexParams()) {
            index++;
            if (param.isRequired()
                    && opt.nonOptionArguments().size() <= index) {
                throw new IllegalArgumentException(param.getName() + " argument was missing.");
            }
            if (opt.nonOptionArguments().size() > index) {
                httpParameters.appendPut(param.getHttpName(), opt.nonOptionArguments().get(index));
            }
        }

        try {
            final String queryString = command.getHttpPath()+GaClientStringUtils.toQueryString(httpParameters, "UTF-8");
            logger.debug("parse COMMAND-LINE[\"{}\"] to QUERY-STRING[\"{}\"]", line, queryString);
            return queryString;
        } catch (UnsupportedEncodingException e) {
            throw new CommandParserException(commandName, e);
        }
    }

    // 根据命令找到对应的命令对象
    private Command foundCommand(final String cmdName) throws CommandNotFoundException {
        final Command command = commands.get(cmdName);
        if (null == command) {
            throw new CommandNotFoundException(cmdName);
        }
        return command;
    }

    // 获取命令对应的参数解析器
    private OptionParser getOptionParser(final Command command) {
        final OptionParser parser = new OptionParser();
        for (final Param param : command.getNamedParams().values()) {
            final OptionSpecBuilder osb = parser.accepts(param.getName(), param.getSummary());
            if (param.isHasValue()) {
                osb.withOptionalArg();
            }
        }
        return parser;
    }

    public static void main(String... args) throws CommandParserException, CommandNotFoundException, CommandInitializationException {
        final CommandParser parser = new CommandParser();
        System.out.println(parser.parse("watch -b -e -s *StringUtils isBlank 'params[0]' 'params.length > 1'"));
        System.out.println(parser.parse("watch -b -e -s *StringUtils 我就无语了 'params[0]'"));
        System.out.println(parser.parse("version"));
    }

}
