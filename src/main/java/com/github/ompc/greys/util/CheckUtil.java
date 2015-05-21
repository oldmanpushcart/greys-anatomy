package com.github.ompc.greys.util;

/**
 * 检查工具类
 * Created by vlinux on 15/5/19.
 */
public class CheckUtil {

    /**
     * 比对某个元素是否在集合中<br/>
     *
     * @param e   元素
     * @param s   元素集合
     * @param <E> 元素类型
     * @return <br/>
     * (1,1,2,3)        == true
     * (1,2,3,4)        == false
     * (null,1,null,2)  == true
     * (1,null)         == false
     */
    public static <E> boolean isIn(E e, E... s) {

        if (null != s) {
            for (E es : s) {
                if (isEquals(e, es)) {
                    return true;
                }
            }
        }

        return false;

    }

    /**
     * 比对某个元素是否不在集合中<br/>
     *
     * @param e   元素
     * @param s   元素集合
     * @param <E> 元素类型
     * @return <br/>
     * (1,1,2,3)        == false
     * (1,2,3,4)        == true
     * (null,1,null,2)  == false
     * (1,null)         == true
     */
    public static <E> boolean isNotIn(E e, E... s) {
        return !isIn(e, s);
    }

    /**
     * 比对两个对象是否相等<br/>
     *
     * @param src    源对象
     * @param target 目标对象
     * @param <E>    对象类型
     * @return <br/>
     * (null, null)    == true
     * (1L,2L)         == false
     * (1L,1L)         == true
     * ("abc",null)    == false
     * (null,"abc")    == false
     */
    public static <E> boolean isEquals(E src, E target) {

        return null == src
                && null == target
                || null != src
                && null != target
                && src.equals(target);

    }

}
