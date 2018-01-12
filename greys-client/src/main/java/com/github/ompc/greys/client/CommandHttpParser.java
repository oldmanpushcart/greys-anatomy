package com.github.ompc.greys.client;

import static com.github.ompc.greys.client.util.GaClientStringUtils.splitForLine;

/**
 * 命令解析器
 * <p>
 * 用于将一个命令字符串解析成可以被后端执行的URL
 * </p>
 */
public class CommandHttpParser {

    private static final String MAPPING_RES = "/com/github/ompc/greys/client/res/command-http-mapping.xml";


    /**
     * 转换成为HTTP的QueryString格式
     *
     * @param line 命令行
     * @return line所对应的QueryString
     * @throws CommandNotFoundException 命令不存在
     * @throws IllegalArgumentException 命令参数非法
     */
    public static String toQueryString(final String line)
            throws CommandNotFoundException, IllegalArgumentException {

        // 拆分命令行
        final String[] lineSegmentArray = splitForLine(line);
        return null;
    }

}
