package com.github.ompc.greys.module.util;

import java.util.concurrent.atomic.AtomicInteger;

import static com.github.ompc.greys.module.util.TryAccess.TryState.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * 尝试访问控制
 *
 * @author oldmanpushcart@gmail.com
 */
public class TryAccess {

    private final String condition;
    private final Integer limit;
    private final boolean isHasLimited;
    private final boolean isHasCondition;
    private final AtomicInteger tokenRef = new AtomicInteger();

    public TryAccess(final String condition,
                     final Integer limit) {
        this.condition = condition;
        this.limit = limit;
        this.isHasLimited = null != limit;
        this.isHasCondition = isNotBlank(condition);
    }


    /*
     * 尝试进行表达式条件判断
     */
    private boolean tryCondition(Express express) throws Express.ExpressException {
        return !isHasCondition || express.is(condition);
    }


    private TryState doTryAccess(final Express express) throws Express.ExpressException {

        // 条件不符合你就不用进来了
        if (!tryCondition(express)) {
            return DENIED;
        }

        // 如果没有做次数控制，直接返回可进入
        if (!isHasLimited) {
            return ACCESS;
        }

        // 需要进行次数控制
        while (true) {

            final int oriToken = tokenRef.get();

            // 被限制了次数
            if (oriToken >= limit) {
                return DENIED;
            }

            // 尝试去挂载令牌，直至成功
            final int newToken = oriToken + 1;
            if (!tokenRef.compareAndSet(oriToken, newToken)) {
                continue;
            }

            // 挂到了令牌之后，需要判断是否最后一个令牌
            return newToken == limit
                    ? ACCESS_LIMITED
                    : ACCESS;

        }
    }

    /**
     * 尝试访问
     *
     * @param express 访问条件
     * @return TRUE:访问成功;FALSE:访问失败
     * @throws Express.ExpressException 条件表达式错误
     * @throws TryAccessException       尝试访问回调中发生错误
     */
    public boolean tryAccess(final Express express) throws Express.ExpressException, TryAccessException {
        switch (doTryAccess(express)) {
            case ACCESS:
                if(null != accessCb) {
                    accessCb.callback();
                }
                return true;
            case ACCESS_LIMITED:
                if(null != accessCb) {
                    accessCb.callback();
                }
                if(null != limitedCb) {
                    limitedCb.callback();
                }
                return true;
            case DENIED:
                if(null != deniedCb) {
                    deniedCb.callback();
                }
            default:
                return false;
        }
    }


    private TryAccessCallback accessCb;
    private TryAccessCallback limitedCb;
    private TryAccessCallback deniedCb;

    public TryAccess onAccess(final TryAccessCallback cb) {
        this.accessCb = cb;
        return this;
    }

    public TryAccess onLimited(final TryAccessCallback cb) {
        this.limitedCb = cb;
        return this;
    }

    public TryAccess onDenied(final TryAccessCallback cb) {
        this.deniedCb = cb;
        return this;
    }

    public interface TryAccessCallback {
        void callback() throws TryAccessException;
    }


    public static class TryAccessException extends Exception {
        public TryAccessException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * 尝试结果
     */
    public enum TryState {

        /**
         * 允许访问
         */
        ACCESS,

        /**
         * 允许访问，但这次最后一次
         */
        ACCESS_LIMITED,

        /**
         * 拒绝访问(条件不满足或已经到达访问次数限制)
         */
        DENIED
    }

}
