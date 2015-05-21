package com.github.ompc.greys.util;

import com.github.ompc.greys.exception.ExpressException;
import ognl.DefaultMemberAccess;
import ognl.Ognl;
import ognl.OgnlContext;
import ognl.OgnlException;

import java.util.Map;

/**
 * 表达式
 * Created by vlinux on 15/5/20.
 */
public interface Express {

    /**
     * 根据表达式获取值
     *
     * @param express 表达式
     * @return 表达式运算后的值
     * @throws ExpressException 表达式运算出错
     */
    Object get(String express) throws ExpressException;

    /**
     * 根据表达式判断是与否
     *
     * @param express 表达式
     * @return 表达式运算后的布尔值
     * @throws ExpressException 表达式运算出错
     */
    boolean is(String express) throws ExpressException;


    /**
     * Ognl表达式
     */
    class OgnlExpress implements Express {

        // 执行表达式的对象
        private final Object object;

        public OgnlExpress(Object object) {
            this.object = object;
        }

        @Override
        public Object get(String express) throws ExpressException {
            final Map<String, Object> context = Ognl.createDefaultContext(null);
            context.put(OgnlContext.MEMBER_ACCESS_CONTEXT_KEY, new DefaultMemberAccess(true, true, true));
            try {
                return Ognl.getValue(express, context, object);
            } catch (OgnlException e) {
                throw new ExpressException(express, e);
            }

        }

        @Override
        public boolean is(String express) throws ExpressException {
            final Object ret = get(express);
            return null != ret
                    && ret instanceof Boolean
                    && (Boolean) ret;
        }
    }


}
