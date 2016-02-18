/**
 * 定义全局对象模块
 * global == window
 */
define('global', function () {
    return (function () {
        return this;
    })()
})


/**
 * GREYS模块
 * function watching(listener)
 */
define('greys', function () {

    // 监听器集合
    var listeners = [];

    return {

        watching: function (listener) {
            listeners[listeners.length] = listener;
        },

        create: function (output) {
            for (var index in listeners) {
                var listener = listeners[index];
                if (listener.hasOwnProperty('create')) {
                    listener.create(output);
                }
            }
        },

        destroy: function (output) {
            for (var index in listeners) {
                var listener = listeners[index];
                if (listener.hasOwnProperty('destroy')) {
                    listener.destroy(output);
                }
            }
        },

        before: function (output, advice, context) {
            for (var index in listeners) {
                var listener = listeners[index];
                if (listener.hasOwnProperty('before')) {
                    listener.before(output, advice, context);
                }
            }
        },

        returning: function (output, advice, context) {
            for (var index in listeners) {
                var listener = listeners[index];
                if (listener.hasOwnProperty('returning')) {
                    listener.returning(output, advice, context);
                }
            }
        },

        throwing: function (output, advice, context) {
            for (var index in listeners) {
                var listener = listeners[index];
                if (listener.hasOwnProperty('throwing')) {
                    listener.throwing(output, advice, context);
                }
            }
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