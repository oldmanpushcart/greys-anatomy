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