/**
 * GREYS模块
 * function watching(listener)
 */
define('greys', function () {

    // 监听器集合
    var listeners = [];

    /**
     * 数组forEach遍历
     * @param arr  数组
     * @param func 回调函数
     */
    function arrayForEach(arr, func) {
        for (var index = 0; index < arr.length; index++) {
            func(index, arr[index]);
        }
    }

    return {

        watching: function (listener) {
            listeners[listeners.length] = listener;
        },

        create: function (output) {
            arrayForEach(listeners, function(index, listener){
                if(listener.create) {
                    listener.create(output);
                }
            });
        },

        destroy: function (output) {
            arrayForEach(listeners, function(index, listener){
                if(listener.destroy) {
                    listener.destroy(output);
                }
            });
        },

        before: function (output, advice, context) {
            arrayForEach(listeners, function(index, listener){
                if(listener.before) {
                    listener.before(output, advice, context);
                }
            });
        },

        returning: function (output, advice, context) {
            arrayForEach(listeners, function(index, listener){
                if(listener.returning) {
                    listener.returning(output, advice, context);
                }
            });
        },

        throwing: function (output, advice, context) {
            arrayForEach(listeners, function(index, listener){
                if(listener.throwing) {
                    listener.throwing(output, advice, context);
                }
            });
        },

    }

})

// 向全局对象注入GREYS回调函数
require(['global', 'greys'], function (global, greys) {
    global.__greys_module_create = function (output) {
        greys.create(output);
    }
    global.__greys_module_destroy = function (output) {
        greys.destroy(output);
    }
    global.__greys_module_before = function (output, advice, context) {
        greys.before(output, advice, context);
    }
    global.__greys_module_returning = function (output, advice, context) {
        greys.returning(output, advice, context);
    }
    global.__greys_module_throwing = function (output, advice, context) {
        greys.throwing(output, advice, context);
    }
})