package com.googlecode.greysanatomy.util;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字符串操作工具类
 *
 * @author vlinux
 */
public class GaStringUtils {

    public static final String LINE = "---------------------------------------------------------------\n";

    public static final String ABORT_MSG = "Press Ctrl+D or Ctrl+X to abort current job.";

    /**
     * 进行URL-ENCODE
     *
     * @param str
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String encode(String str) throws UnsupportedEncodingException {
        if (isBlank(str)) {
            return EMPTY;
        }
        return URLEncoder.encode(str, "utf-8");
    }

    /**
     * 进行URL-DECODE
     *
     * @param str
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String decode(String str) throws UnsupportedEncodingException {
        if (isBlank(str)) {
            return EMPTY;
        }
        return URLDecoder.decode(str, "utf-8");
    }

    /**
     * 创建一个字符串
     *
     * @param obj
     * @return
     */
    public static String newString(Object obj) {
        if (null == obj) {
            return EMPTY;
        }
        return obj.toString();
    }

    /**
     * 展示logo
     *
     * @return
     */
    public static String getLogo() {
        final StringBuilder logoSB = new StringBuilder();
        final Scanner scanner = new Scanner(Object.class.getResourceAsStream("/com/googlecode/greysanatomy/res/logo.txt"));
        while (scanner.hasNextLine()) {
            logoSB.append(scanner.nextLine()).append("\n");
        }
        return logoSB.toString();
    }

    /**
     * 右填充
     *
     * @param sb
     * @param c
     * @param str
     */
    public static void rightFill(StringBuilder sb, int c, String str) {
        for (int i = 0; i < c; i++) {
            sb.append(str);
        }
    }

    /**
     * 获取方法执行堆栈信息
     *
     * @return
     */
    public static String getStack() {
        final StackTraceElement[] stes = Thread.currentThread().getStackTrace();
        final StringBuilder stSB = new StringBuilder()
                .append("Thread Info:").append(Thread.currentThread().getName()).append("\n");

        if (null == stes
                || stes.length == 0
                || stes.length == 1) {
            return stSB.toString();
        }

        for (int index = 4; index < stes.length; index++) {
            final StackTraceElement ste = stes[index];
            stSB.append(index == 2 ? "  " : "    at ")
                    .append(ste.getClassName()).append(".")
                    .append(ste.getMethodName())
                    .append("(").append(ste.getFileName()).append(":").append(ste.getLineNumber()).append(")\n");
        }

        return stSB.toString();
    }


    /**
     * 进度条
     *
     * @param name
     * @param progress
     * @param total
     * @return
     */
    public static String progress(String name, int progress, int total) {

        final StringBuilder sb = new StringBuilder();
        final int f = progress * 100 / total;
        sb.append(String.format("%s: %3d", name, f)).append("%[");
        for (int index = 1; index <= 100; index++) {
            if (index <= f) {
                sb.append("#");
            } else {
                sb.append(" ");
            }
        }
        sb.append("]");

        return sb.toString();

    }

    /**
     * 产生摘要
     *
     * @param str    字符串内容
     * @param length 摘要长度
     * @return 字符串的摘要信息
     */
    public static String summary(String str, int length) {

        final StringBuilder sb = new StringBuilder();

        if (length(str) <= length) {
            sb.append(str);
        } else if (length <= 3) {
            sb.append(substring(str, 0, 3));
        } else {
            sb.append(substring(str, 0, length - 3)).append("...");
        }

        return sb.toString();

    }


    // Empty checks
    //-----------------------------------------------------------------------

    /**
     * <p>Checks if a String is empty ("") or null.</p>
     * <p/>
     * <pre>
     * StringUtils.isEmpty(null)      = true
     * StringUtils.isEmpty("")        = true
     * StringUtils.isEmpty(" ")       = false
     * StringUtils.isEmpty("bob")     = false
     * StringUtils.isEmpty("  bob  ") = false
     * </pre>
     * <p/>
     * <p>NOTE: This method changed in Lang version 2.0.
     * It no longer trims the String.
     * That functionality is available in isBlank().</p>
     *
     * @param str the String to check, may be null
     * @return <code>true</code> if the String is empty or null
     */
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }


    /**
     * <p>Checks if a String is whitespace, empty ("") or null.</p>
     * <p/>
     * <pre>
     * StringUtils.isBlank(null)      = true
     * StringUtils.isBlank("")        = true
     * StringUtils.isBlank(" ")       = true
     * StringUtils.isBlank("bob")     = false
     * StringUtils.isBlank("  bob  ") = false
     * </pre>
     *
     * @param str the String to check, may be null
     * @return <code>true</code> if the String is null, empty or whitespace
     * @since 2.0
     */
    public static boolean isBlank(String str) {
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if ((!Character.isWhitespace(str.charAt(i)))) {
                return false;
            }
        }
        return true;
    }

    /**
     * The empty String <code>""</code>.
     *
     * @since 2.0
     */
    public static final String EMPTY = "";

    /**
     * <p>Splits the provided text into an array, separators specified.
     * This is an alternative to using StringTokenizer.</p>
     * <p/>
     * <p>The separator is not included in the returned String array.
     * Adjacent separators are treated as one separator.
     * For more control over the split use the StrTokenizer class.</p>
     * <p/>
     * <p>A <code>null</code> input String returns <code>null</code>.
     * A <code>null</code> separatorChars splits on whitespace.</p>
     * <p/>
     * <pre>
     * StringUtils.split(null, *)         = null
     * StringUtils.split("", *)           = []
     * StringUtils.split("abc def", null) = ["abc", "def"]
     * StringUtils.split("abc def", " ")  = ["abc", "def"]
     * StringUtils.split("abc  def", " ") = ["abc", "def"]
     * StringUtils.split("ab:cd:ef", ":") = ["ab", "cd", "ef"]
     * </pre>
     *
     * @param str            the String to parse, may be null
     * @param separatorChars the characters used as the delimiters,
     *                       <code>null</code> splits on whitespace
     * @return an array of parsed Strings, <code>null</code> if null String input
     */
    public static String[] split(String str, String separatorChars) {
        final String[] stringSegments = splitWorker(str, separatorChars, -1, false);
        return null == stringSegments ? new String[]{} : stringSegments;
    }

    /**
     * An empty immutable <code>String</code> array.
     */
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * Performs the logic for the <code>split</code> and
     * <code>splitPreserveAllTokens</code> methods that return a maximum array
     * length.
     *
     * @param str               the String to parse, may be <code>null</code>
     * @param separatorChars    the separate character
     * @param max               the maximum number of elements to include in the
     *                          array. A zero or negative value implies no limit.
     * @param preserveAllTokens if <code>true</code>, adjacent separators are
     *                          treated as empty token separators; if <code>false</code>, adjacent
     *                          separators are treated as one separator.
     * @return an array of parsed Strings, <code>null</code> if null String input
     */
    private static String[] splitWorker(String str, String separatorChars, int max, boolean preserveAllTokens) {
        // Performance tuned for 2.0 (JDK1.4)
        // Direct code is quicker than StringTokenizer.
        // Also, StringTokenizer uses isSpace() not isWhitespace()

        if (str == null) {
            return null;
        }
        int len = str.length();
        if (len == 0) {
            return EMPTY_STRING_ARRAY;
        }
        List<String> list = new ArrayList<String>();
        int sizePlus1 = 1;
        int i = 0, start = 0;
        boolean match = false;
        boolean lastMatch = false;
        if (separatorChars == null) {
            // Null separator means use whitespace
            while (i < len) {
                if (Character.isWhitespace(str.charAt(i))) {
                    if (match || preserveAllTokens) {
                        lastMatch = true;
                        if (sizePlus1++ == max) {
                            i = len;
                            lastMatch = false;
                        }
                        list.add(str.substring(start, i));
                        match = false;
                    }
                    start = ++i;
                    continue;
                }
                lastMatch = false;
                match = true;
                i++;
            }
        } else if (separatorChars.length() == 1) {
            // Optimise 1 character case
            char sep = separatorChars.charAt(0);
            while (i < len) {
                if (str.charAt(i) == sep) {
                    if (match || preserveAllTokens) {
                        lastMatch = true;
                        if (sizePlus1++ == max) {
                            i = len;
                            lastMatch = false;
                        }
                        list.add(str.substring(start, i));
                        match = false;
                    }
                    start = ++i;
                    continue;
                }
                lastMatch = false;
                match = true;
                i++;
            }
        } else {
            // standard case
            while (i < len) {
                if (separatorChars.indexOf(str.charAt(i)) >= 0) {
                    if (match || preserveAllTokens) {
                        lastMatch = true;
                        if (sizePlus1++ == max) {
                            i = len;
                            lastMatch = false;
                        }
                        list.add(str.substring(start, i));
                        match = false;
                    }
                    start = ++i;
                    continue;
                }
                lastMatch = false;
                match = true;
                i++;
            }
        }
        if (match || (preserveAllTokens && lastMatch)) {
            list.add(str.substring(start, i));
        }


        return list.toArray(new String[list.size()]);
    }

    // Equals
    //-----------------------------------------------------------------------

    /**
     * <p>Compares two Strings, returning <code>true</code> if they are equal.</p>
     * <p/>
     * <p><code>null</code>s are handled without exceptions. Two <code>null</code>
     * references are considered to be equal. The comparison is case sensitive.</p>
     * <p/>
     * <pre>
     * StringUtils.equals(null, null)   = true
     * StringUtils.equals(null, "abc")  = false
     * StringUtils.equals("abc", null)  = false
     * StringUtils.equals("abc", "abc") = true
     * StringUtils.equals("abc", "ABC") = false
     * </pre>
     *
     * @param str1 the first String, may be null
     * @param str2 the second String, may be null
     * @return <code>true</code> if the Strings are equal, case sensitive, or
     * both <code>null</code>
     * @see java.lang.String#equals(Object)
     */
    public static boolean equals(String str1, String str2) {
        return str1 == null ? str2 == null : str1.equals(str2);
    }

    /**
     * <p>Checks if a String is not empty (""), not null and not whitespace only.</p>
     * <p/>
     * <pre>
     * StringUtils.isNotBlank(null)      = false
     * StringUtils.isNotBlank("")        = false
     * StringUtils.isNotBlank(" ")       = false
     * StringUtils.isNotBlank("bob")     = true
     * StringUtils.isNotBlank("  bob  ") = true
     * </pre>
     *
     * @param str the String to check, may be null
     * @return <code>true</code> if the String is
     * not empty and not null and not whitespace
     * @since 2.0
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * <p>The maximum size to which the padding constant(s) can expand.</p>
     */
    private static final int PAD_LIMIT = 8192;

    // Padding
    //-----------------------------------------------------------------------

    /**
     * <p>Repeat a String <code>repeat</code> times to form a
     * new String.</p>
     * <p/>
     * <pre>
     * StringUtils.repeat(null, 2) = null
     * StringUtils.repeat("", 0)   = ""
     * StringUtils.repeat("", 2)   = ""
     * StringUtils.repeat("a", 3)  = "aaa"
     * StringUtils.repeat("ab", 2) = "abab"
     * StringUtils.repeat("a", -2) = ""
     * </pre>
     *
     * @param str    the String to repeat, may be null
     * @param repeat number of times to repeat str, negative treated as zero
     * @return a new String consisting of the original String repeated,
     * <code>null</code> if null String input
     */
    public static String repeat(String str, int repeat) {
        // Performance tuned for 2.0 (JDK1.4)

        if (str == null) {
            return null;
        }
        if (repeat <= 0) {
            return EMPTY;
        }
        int inputLength = str.length();
        if (repeat == 1 || inputLength == 0) {
            return str;
        }
        if (inputLength == 1 && repeat <= PAD_LIMIT) {
            return padding(repeat, str.charAt(0));
        }

        int outputLength = inputLength * repeat;
        switch (inputLength) {
            case 1:
                char ch = str.charAt(0);
                char[] output1 = new char[outputLength];
                for (int i = repeat - 1; i >= 0; i--) {
                    output1[i] = ch;
                }
                return new String(output1);
            case 2:
                char ch0 = str.charAt(0);
                char ch1 = str.charAt(1);
                char[] output2 = new char[outputLength];
                for (int i = repeat * 2 - 2; i >= 0; i--, i--) {
                    output2[i] = ch0;
                    output2[i + 1] = ch1;
                }
                return new String(output2);
            default:
//                StrBuilder buf = new StrBuilder(outputLength);
//                for (int i = 0; i < repeat; i++) {
//                    buf.append(str);
//                }
//                return buf.toString();

                final StringBuilder buf = new StringBuilder();
                for (int i = 0; i < repeat; i++) {
                    buf.append(str);
                }
                return buf.toString();
        }
    }

    /**
     * <p>Returns padding using the specified delimiter repeated
     * to a given length.</p>
     * <p/>
     * <pre>
     * StringUtils.padding(0, 'e')  = ""
     * StringUtils.padding(3, 'e')  = "eee"
     * StringUtils.padding(-2, 'e') = IndexOutOfBoundsException
     * </pre>
     * <p/>
     * <p>Note: this method doesn't not support padding with
     * <a href="http://www.unicode.org/glossary/#supplementary_character">Unicode Supplementary Characters</a>
     * as they require a pair of <code>char</code>s to be represented.
     * If you are needing to support full I18N of your applications
     * consider using {@link #repeat(String, int)} instead.
     * </p>
     *
     * @param repeat  number of times to repeat delim
     * @param padChar character to repeat
     * @return String with repeated character
     * @throws IndexOutOfBoundsException if <code>repeat &lt; 0</code>
     * @see #repeat(String, int)
     */
    private static String padding(int repeat, char padChar) throws IndexOutOfBoundsException {
        if (repeat < 0) {
            throw new IndexOutOfBoundsException("Cannot pad a negative amount: " + repeat);
        }
        final char[] buf = new char[repeat];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = padChar;
        }
        return new String(buf);
    }

    /**
     * <p>Gets a substring from the specified String avoiding exceptions.</p>
     * <p/>
     * <p>A negative start position can be used to start/end <code>n</code>
     * characters from the end of the String.</p>
     * <p/>
     * <p>The returned substring starts with the character in the <code>start</code>
     * position and ends before the <code>end</code> position. All position counting is
     * zero-based -- i.e., to start at the beginning of the string use
     * <code>start = 0</code>. Negative start and end positions can be used to
     * specify offsets relative to the end of the String.</p>
     * <p/>
     * <p>If <code>start</code> is not strictly to the left of <code>end</code>, ""
     * is returned.</p>
     * <p/>
     * <pre>
     * StringUtils.substring(null, *, *)    = null
     * StringUtils.substring("", * ,  *)    = "";
     * StringUtils.substring("abc", 0, 2)   = "ab"
     * StringUtils.substring("abc", 2, 0)   = ""
     * StringUtils.substring("abc", 2, 4)   = "c"
     * StringUtils.substring("abc", 4, 6)   = ""
     * StringUtils.substring("abc", 2, 2)   = ""
     * StringUtils.substring("abc", -2, -1) = "b"
     * StringUtils.substring("abc", -4, 2)  = "ab"
     * </pre>
     *
     * @param str   the String to get the substring from, may be null
     * @param start the position to start from, negative means
     *              count back from the end of the String by this many characters
     * @param end   the position to end at (exclusive), negative means
     *              count back from the end of the String by this many characters
     * @return substring from start position to end positon,
     * <code>null</code> if null String input
     */
    public static String substring(String str, int start, int end) {
        if (str == null) {
            return null;
        }

        // handle negatives
        if (end < 0) {
            end = str.length() + end; // remember end is negative
        }
        if (start < 0) {
            start = str.length() + start; // remember start is negative
        }

        // check length next
        if (end > str.length()) {
            end = str.length();
        }

        // if start is greater than end, return ""
        if (start > end) {
            return EMPTY;
        }

        if (start < 0) {
            start = 0;
        }
        if (end < 0) {
            end = 0;
        }

        return str.substring(start, end);
    }

    /**
     * Represents a failed index search.
     *
     * @since 2.1
     */
    public static final int INDEX_NOT_FOUND = -1;

    /**
     * <p>Gets the substring after the last occurrence of a separator.
     * The separator is not returned.</p>
     * <p/>
     * <p>A <code>null</code> string input will return <code>null</code>.
     * An empty ("") string input will return the empty string.
     * An empty or <code>null</code> separator will return the empty string if
     * the input string is not <code>null</code>.</p>
     * <p/>
     * <p>If nothing is found, the empty string is returned.</p>
     * <p/>
     * <pre>
     * StringUtils.substringAfterLast(null, *)      = null
     * StringUtils.substringAfterLast("", *)        = ""
     * StringUtils.substringAfterLast(*, "")        = ""
     * StringUtils.substringAfterLast(*, null)      = ""
     * StringUtils.substringAfterLast("abc", "a")   = "bc"
     * StringUtils.substringAfterLast("abcba", "b") = "a"
     * StringUtils.substringAfterLast("abc", "c")   = ""
     * StringUtils.substringAfterLast("a", "a")     = ""
     * StringUtils.substringAfterLast("a", "z")     = ""
     * </pre>
     *
     * @param str       the String to get a substring from, may be null
     * @param separator the String to search for, may be null
     * @return the substring after the last occurrence of the separator,
     * <code>null</code> if null String input
     * @since 2.0
     */
    public static String substringAfterLast(String str, String separator) {
        if (isEmpty(str)) {
            return str;
        }
        if (isEmpty(separator)) {
            return EMPTY;
        }
        int pos = str.lastIndexOf(separator);
        if (pos == INDEX_NOT_FOUND || pos == (str.length() - separator.length())) {
            return EMPTY;
        }
        return str.substring(pos + separator.length());
    }

    /**
     * Gets a String's length or <code>0</code> if the String is <code>null</code>.
     *
     * @param str a String or <code>null</code>
     * @return String length or <code>0</code> if the String is <code>null</code>.
     * @since 2.4
     */
    public static int length(String str) {
        return str == null ? 0 : str.length();
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
     * 拆分参数，要求能将命令行字符串拆分成为多个数组
     *
     * @param argumentString
     * @return
     */
    public static String[] splitForArgument(String argumentString) {
        Pattern compile = Pattern.compile("(\"[^\"]*\")|('[^']*')|([^\\s+]+)");
        Matcher matcher = compile.matcher(argumentString);

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
     * 命令提示符
     */
    public static final String DEFAULT_PROMPT = "\rga?>";


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

}
