package com.github.ompc.greys.core.command;

/**
 * 脚本支持命令
 * Created by oldmanpushcart@gmail.com on 15/6/1.
 */
public interface ScriptSupportCommand {

    /**
     * 输出器
     */
    interface Output {

        /**
         * 输出字符串(不换行)
         *
         * @param string 待输出字符串
         * @return this
         */
        Output print(String string);

        /**
         * 输出字符串(换行)
         *
         * @param string 待输出字符串
         * @return this
         */
        Output println(String string);

        /**
         * 结束当前脚本
         *
         * @return this
         */
        Output finish();

    }

}
