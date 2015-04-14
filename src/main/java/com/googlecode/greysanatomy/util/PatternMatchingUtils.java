package com.googlecode.greysanatomy.util;

/**
 * 模式匹配工具类
 */
public class PatternMatchingUtils {

    /**
     * 进行模式匹配
     *
     * @param string
     * @param pattern
     * @param isRegEx
     * @return
     */
    public static boolean matching(String string, String pattern, boolean isRegEx) {

        if (isRegEx) {
            return string.matches(pattern);
        } else {
            return WildcardMatchingUtils.match(string, pattern);
        }

    }
}
