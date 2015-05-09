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

}
