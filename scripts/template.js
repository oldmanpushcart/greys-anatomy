/**
 * 模版
 */
require(['greys'], function (greys) {
    greys.watching({

        /**
         * 脚本创建函数
         * 在脚本第一次运行时候执行，可以在这个函数中进行脚本初始化工作
         * @param output 输出器
         */
        create: function (output) {

        },

        /**
         * 脚本销毁函数
         * 在脚本运行完成时候执行，可以在这个函数中进行脚本销毁工作
         * @param output 输出器
         */
        destroy: function (output) {

        },

        /**
         * 方法执行前回调函数
         * 在Java方法执行之前执行该函数
         * @param output    输出器
         * @param advice    通知点
         * @param context   方法执行上下文(线程安全)
         */
        before: function (output, advice, context) {

        },

        /**
         * 方法返回回调函数
         * 在Java方法执行成功之后，Java方法返回之前执行该函数
         * @param output    输出器
         * @param advice    通知点
         * @param context   方法执行上下文(线程安全)
         */
        returning: function (output, advice, context) {

        },

        /**
         * 方法抛异常回调函数
         * 在Java方法内部执行抛异常之后，Java方法对外抛异常之前执行该函数
         * @param output    输出器
         * @param advice    通知点
         * @param context   方法执行上下文(线程安全)
         */
        throwing: function (output, advice, context) {

        },

    });
})