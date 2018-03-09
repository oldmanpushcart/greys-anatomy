package com.github.ompc.greys.protocol.impl.v1;

import com.github.ompc.greys.protocol.Gp;
import com.github.ompc.greys.protocol.GpType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Gp(GpType.TRACE)
public class Trace {

    private final String title;
    private final List<Call> calls = new ArrayList<Call>();

    @Data
    public static class Call {

        private final long callCostMs;
        private final int callDeep;
        private final String callClassName;
        private final String callBehaviorName;
        private final String callBehaviorDesc;
        private final boolean isCallThrows;
        private final String callThrowExceptionClassName;

        /**
         * 构造一个正常返回的Trace$Call
         *
         * @param callCostMs       调用耗时(毫秒)
         * @param callDeep         调用深度
         * @param callClassName    调用类名
         * @param callBehaviorName 调用行为名
         * @param callBehaviorDesc 调用行为描述(ASM)
         * @return 一次调用描述封装
         */
        public static Call makeReturn(final long callCostMs,
                                      final int callDeep,
                                      final String callClassName,
                                      final String callBehaviorName,
                                      final String callBehaviorDesc) {
            return new Call(
                    callCostMs,
                    callDeep,
                    callClassName,
                    callBehaviorName,
                    callBehaviorDesc,
                    false,
                    null
            );
        }

        /**
         * 构造一个抛出异常的Trace$Call
         *
         * @param callCostMs                  调用耗时(毫秒)
         * @param callDeep                    调用深度
         * @param callClassName               调用类名
         * @param callBehaviorName            调用行为名
         * @param callBehaviorDesc            调用行为描述(ASM)
         * @param callThrowExceptionClassName 调用抛出的异常类名
         * @return 一次调用描述封装
         */
        public static Call makeThrows(final long callCostMs,
                                      final int callDeep,
                                      final String callClassName,
                                      final String callBehaviorName,
                                      final String callBehaviorDesc,
                                      String callThrowExceptionClassName) {
            return new Call(
                    callCostMs,
                    callDeep,
                    callClassName,
                    callBehaviorName,
                    callBehaviorDesc,
                    true,
                    callThrowExceptionClassName
            );
        }

    }

}
