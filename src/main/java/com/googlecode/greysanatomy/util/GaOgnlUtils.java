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
        final Map<String, Object> context = Ognl.createDefaultContext(null);
        context.put(OgnlContext.MEMBER_ACCESS_CONTEXT_KEY, new DefaultMemberAccess(true, true, true));
        return Ognl.getValue(express, context, object);
    }

}
