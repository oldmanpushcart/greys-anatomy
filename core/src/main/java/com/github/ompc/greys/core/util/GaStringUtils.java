package com.github.ompc.greys.core.util;

import com.github.ompc.greys.core.view.TableView;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * 字符串工具类
 * Created by vlinux on 15/5/18.
 */
public class GaStringUtils {

    /**
     * 命令提示符
     */
    public static final String DEFAULT_PROMPT = "ga?>";

    /**
     * 中断提示
     */
    public static final String ABORT_MSG = "Press Ctrl+D or Ctrl+X to abort.";

    /**
     * 拆分参数，要求能将命令行字符串拆分成为多个数组
     *
     * @param argumentString 参数行
     * @return 拆分后的参数数组
     */
    public static String[] splitForArgument(String argumentString) {

        Pattern compile = Pattern.compile("(\"[^\"]*\")|('[^']*')|([^\\s+]+)");
        java.util.regex.Matcher matcher = compile.matcher(argumentString);

        final ArrayList<String> stringList = new ArrayList<String>();
        while (matcher.find()) {

            final String segment = matcher.group();
            if (segment.length() > 1
                    && ((segment.startsWith("'") && segment.endsWith("'")) || (segment.startsWith("\"") && segment.endsWith("\"")))) {
                stringList.add(segment.replaceAll("(^['|\"])|(['|\"]$)", ""));
            } else {
                stringList.add(segment);
            }

        }

        return stringList.toArray(new String[stringList.size()]);

    }


    /**
     * 获取异常的原因描述
     *
     * @param t 异常
     * @return 异常原因
     */
    public static String getCauseMessage(Throwable t) {
        if (null != t.getCause()) {
            return getCauseMessage(t.getCause());
        }
        return t.getMessage();
    }

    /**
     * 展示logo<br/>
     * 这个代码不忍直视，忍吧，不要问我怎么写出来的
     *
     * @return Logo信息
     * @throws IOException resource not found.
     */
    public static String getLogo() throws IOException {
        final String logo = IOUtils.toString(GaStringUtils.class.getResourceAsStream("/com/github/ompc/greys/core/res/logo.txt"));
        final String version = IOUtils.toString(GaStringUtils.class.getResourceAsStream("/com/github/ompc/greys/core/res/version"));

        final char[] versionPrefixArray = "version:".toCharArray();
        final String[] versionArray = StringUtils.split(version, '.');
        final int FIX_VERSION_LENGTH = 4;
        final Object[] objectArray = new Object[versionPrefixArray.length + versionArray.length + FIX_VERSION_LENGTH];
        for (int index = 0; index < versionPrefixArray.length; index++) {
            objectArray[index] = versionPrefixArray[index];
        }

        for (int index = versionPrefixArray.length; index < objectArray.length; index += 2) {
            objectArray[index] = versionArray[(index - versionPrefixArray.length) / 2];
            if (index < objectArray.length) {
                objectArray[index + 1] = ".";
            }
        }
        return new TableView(new TableView.ColumnDefine[]{new TableView.ColumnDefine(TableView.Align.RIGHT)})
                .addRow(new TableView(1).addRow(logo).draw())
                .addRow(new TableView(15).addRow(objectArray).hasBorder(true).draw())
                .draw();
    }

    /**
     * 将一个对象转换为字符串
     *
     * @param obj 目标对象
     * @return 字符串
     */
    public static String newString(Object obj) {
        if (null == obj) {
            return EMPTY;
        }
        return obj.toString();
    }

    /**
     * 翻译类名称
     *
     * @param clazz Java类
     * @return 翻译值
     */
    public static String tranClassName(Class<?> clazz) {
        if (clazz.isArray()) {
            StringBuilder sb = new StringBuilder(clazz.getName());
            sb.delete(0, 2);
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ';') {
                sb.deleteCharAt(sb.length() - 1);
            }
            sb.append("[]");
            return sb.toString();
        } else {
            return clazz.getName();
        }
    }


    /**
     * 翻译类名称<br/>
     * 将 java/lang/String 的名称翻译成 java.lang.String
     *
     * @param className 类名称 java/lang/String
     * @return 翻译后名称 java.lang.String
     */
    public static String tranClassName(String className) {
        return StringUtils.replace(className, "/", ".");
    }

    /**
     * 翻译Modifier值
     *
     * @param mod modifier
     * @return 翻译值
     */
    public static String tranModifier(int mod) {
        StringBuilder sb = new StringBuilder();
        if (Modifier.isAbstract(mod)) {
            sb.append("abstract,");
        }
        if (Modifier.isFinal(mod)) {
            sb.append("final,");
        }
        if (Modifier.isInterface(mod)) {
            sb.append("interface,");
        }
        if (Modifier.isNative(mod)) {
            sb.append("native,");
        }
        if (Modifier.isPrivate(mod)) {
            sb.append("private,");
        }
        if (Modifier.isProtected(mod)) {
            sb.append("protected,");
        }
        if (Modifier.isPublic(mod)) {
            sb.append("public,");
        }
        if (Modifier.isStatic(mod)) {
            sb.append("static,");
        }
        if (Modifier.isStrict(mod)) {
            sb.append("strict,");
        }
        if (Modifier.isSynchronized(mod)) {
            sb.append("synchronized,");
        }
        if (Modifier.isTransient(mod)) {
            sb.append("transient,");
        }
        if (Modifier.isVolatile(mod)) {
            sb.append("volatile,");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }


    /**
     * 获取方法执行堆栈信息
     *
     * @return 方法堆栈信息
     */
    public static String getStack() {

        final Thread currentThread = Thread.currentThread();
        final StackTraceElement[] stackTraceElementArray = currentThread.getStackTrace();

        final String title = String.format("thread_name=\"%s\" thread_id=0x%s;is_daemon=%s;priority=%s;",
                currentThread.getName(),
                Long.toHexString(currentThread.getId()),
                currentThread.isDaemon(),
                currentThread.getPriority());

        final StackTraceElement locationStackTraceElement = stackTraceElementArray[10];
        final String locationString = String.format("    @%s.%s()",
                locationStackTraceElement.getClassName(),
                locationStackTraceElement.getMethodName());

        final StringBuilder stSB = new StringBuilder()
                .append(title).append("\n")
                .append(locationString).append("\n");

        final int skip = 11;
        for (int index = skip; index < stackTraceElementArray.length; index++) {
            final StackTraceElement ste = stackTraceElementArray[index];
            stSB
                    .append("        at ")
                    .append(ste.getClassName()).append(".")
                    .append(ste.getMethodName())
                    .append("(").append(ste.getFileName()).append(":").append(ste.getLineNumber()).append(")\n");
        }

        return stSB.toString();
    }

    /**
     * 自动换行
     *
     * @param string 字符串
     * @param width  行宽
     * @return 换行后的字符串
     */
    public static String wrap(String string, int width) {
        final StringBuilder sb = new StringBuilder();
        final char[] buffer = string.toCharArray();
        int count = 0;
        for (char c : buffer) {

            if (count++ == width) {
                count = 0;
                sb.append('\n');
                if (c == '\n') {
                    continue;
                }
            }

            if (c == '\n') {
                count = 0;
            }

            sb.append(c);

        }
        return sb.toString();
    }

}

