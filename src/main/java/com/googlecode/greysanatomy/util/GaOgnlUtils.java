package com.googlecode.greysanatomy.util;

import ognl.DefaultMemberAccess;
import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;

import java.util.Map;

/**
 * Ga的Ognl工具
 * Created by vlinux on 14/12/10.
 */
public class GaOgnlUtils {

    /**
     * 获取ognl表达式的值
     *
     * @param express OGNL表达式
     * @param object  获取的数据对象
     * @return 期望的值
     * @throws OgnlException 获取异常
     */
    public static Object getValue(String express, Object object) throws OgnlException {
        @SuppressWarnings("unchecked") final Map<String, Object> context = Ognl.createDefaultContext(null);
        context.put(OgnlContext.MEMBER_ACCESS_CONTEXT_KEY, new DefaultMemberAccess(true, true, true));
        return Ognl.getValue(express, context, object);
    }


    /**
     * 使用ognl表达式做条件匹配
     *
     * @param conditionExpress OGNL条件表达式
     * @param object           进行条件匹配的对象
     * @return 匹配结果
     * @throws OgnlException 匹配异常
     */
    public static boolean is(String conditionExpress, Object object) throws OgnlException {
        final Object ret =  getValue(conditionExpress, object);

        return null != ret
                && ret instanceof Boolean
                && (Boolean)ret;

    }

}
