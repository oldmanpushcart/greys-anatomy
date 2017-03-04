package com.github.ompc.greys.core.util;

/**
 * Greys字符串处理工具类
 * Created by vlinux on 2017/3/2.
 */
public class GaStringUtils {

    /**
     * 通配符表达式匹配
     *
     * @param string  目标字符串
     * @param pattern 匹配模版
     * @return true:目标字符串符合匹配模版;false:目标字符串不符合匹配模版
     */
    public static boolean matching(String string, String pattern) {
        return match(string, pattern, 0, 0);
    }


    /**
     * Internal matching recursive function.
     */
    private static boolean match(String string, String pattern, int stringStartNdx, int patternStartNdx) {
        int pNdx = patternStartNdx;
        int sNdx = stringStartNdx;
        int pLen = pattern.length();
        if (pLen == 1) {
            if (pattern.charAt(0) == '*') {     // speed-up
                return true;
            }
        }
        int sLen = string.length();
        boolean nextIsNotWildcard = false;

        while (true) {

            // check if end of string and/or pattern occurred
            if ((sNdx >= sLen)) {   // end of string still may have pending '*' callback pattern
                while ((pNdx < pLen) && (pattern.charAt(pNdx) == '*')) {
                    pNdx++;
                }
                return pNdx >= pLen;
            }
            if (pNdx >= pLen) {         // end of pattern, but not end of the string
                return false;
            }
            char p = pattern.charAt(pNdx);    // pattern char

            // perform logic
            if (!nextIsNotWildcard) {

                if (p == '\\') {
                    pNdx++;
                    nextIsNotWildcard = true;
                    continue;
                }
                if (p == '?') {
                    sNdx++;
                    pNdx++;
                    continue;
                }
                if (p == '*') {
                    char pnext = 0;           // next pattern char
                    if (pNdx + 1 < pLen) {
                        pnext = pattern.charAt(pNdx + 1);
                    }
                    if (pnext == '*') {         // double '*' have the same effect as one '*'
                        pNdx++;
                        continue;
                    }
                    int i;
                    pNdx++;

                    // find recursively if there is any substring from the end of the
                    // line that matches the rest of the pattern !!!
                    for (i = string.length(); i >= sNdx; i--) {
                        if (match(string, pattern, i, pNdx)) {
                            return true;
                        }
                    }
                    return false;
                }
            } else {
                nextIsNotWildcard = false;
            }

            // check if pattern char and string char are equals
            if (p != string.charAt(sNdx)) {
                return false;
            }

            // everything matches for now, continue
            sNdx++;
            pNdx++;
        }
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

}
