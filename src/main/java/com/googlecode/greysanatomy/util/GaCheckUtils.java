package com.googlecode.greysanatomy.util;

/**
 * 检车工具类
 *
 * @author vlinux
 */
public class GaCheckUtils {

    /**
     * 判断某个值是否在某片值范围之内
     *
     * @param <E>
     * @param e
     * @param collections
     * @return
     */
    public static <E> boolean isIn(E e, E... collections) {
        if (null == collections || collections.length == 0) {
            return false;
        }//if
        for (E ce : collections) {
            if ((null == e && null == ce)
                    || (null != e && null != ce && e.equals(ce))) {
                return true;
            }//if
        }//for
        return false;
    }

    /**
     * 大小写无关的判断某个字符串是否在某片字符串值范围之内
     *
     * @param s
     * @param strs
     * @return
     */
    public static boolean isInIgnoreCase(String s, String... strs) {
        if (null == strs || strs.length == 0) {
            return false;
        }//if
        for (String str : strs) {
            if ((null == str && null == s)
                    || (null != s && null != str && str.equalsIgnoreCase(s))) {
                return true;
            }//if
        }//for
        return false;
    }

}
