/**
 * GREYS模块
 * function watching(listener)
 */
define('greys', function () {

    // 监听器集合
    var _listeners = [];

    // 带匹配需求的监听器集合
    var _regex_listeners = [];

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

    function watching() {
        // watching(listener)
        if (arguments.length == 1
            && arguments[0] instanceof Object) {
            _listeners.push(arguments[0]);
        }

        // watching(/class_name_regex/, listener)
        else if (arguments.length == 2
            && arguments[0] instanceof RegExp
            && arguments[1] instanceof Object) {
            watching(arguments[0], /.*/, arguments[1]);
        }

        // watching(/class_name_regex/, /method_name_regex/, listener)
        else if (arguments.length == 3
            && arguments[0] instanceof RegExp
            && arguments[1] instanceof RegExp
            && arguments[2] instanceof Object) {
            _regex_listeners.push({
                testClass: arguments[0],
                testMethod: arguments[1],
                listener: arguments[2]
            });
        }
    }

    return {

        /**
         * 添加监听器,凡是被js命令所拦截到的方法都会流入到注册的监听器中
         *  watching(listener)
         *  watching(/class_name_regex/,/method_name_regex/,listener)
         *  watching(/class_name_regex/,listener)
         */
        watching: watching,

        testJavaClassName: function (javaClassName) {
            for (var index in _regex_listeners) {
                var _regex_listener = _regex_listeners[index];
                if (_regex_listener
                    && _regex_listener.testClass.test(javaClassName)) {
                    return true;
                }
            }//for
            return false;
        },

        testJavaMethodName: function (javaMethodName) {
            for (var index in _regex_listeners) {
                var _regex_listener = _regex_listeners[index];
                if (_regex_listener
                    && _regex_listener.testMethod.test(javaMethodName)) {
                    return true;
                }
            }//for
            return false;
        },

        create: function (output) {
            arrayForEach(_listeners, function (index, listener) {
                if (listener.create) {
                    listener.create(output);
                }
            });
            arrayForEach(_regex_listeners, function (index, _regex_listener) {
                if (_regex_listener.listener.create) {
                    _regex_listener.listener.create(output);
                }
            });
        },

        destroy: function (output) {
            arrayForEach(_listeners, function (index, listener) {
                if (listener.destroy) {
                    listener.destroy(output);
                }
            });
            arrayForEach(_regex_listeners, function (index, _regex_listener) {
                if (_regex_listener.listener.destroy) {
                    _regex_listener.listener.destroy(output);
                }
            });
        },

        before: function (output, advice, context) {
            arrayForEach(_listeners, function (index, listener) {
                if (listener.before) {
                    listener.before(output, advice, context);
                }
            });
            arrayForEach(_regex_listeners, function (index, _regex_listener) {
                if (_regex_listener.listener.before) {
                    _regex_listener.listener.before(output, advice, context);
                }
            });
        },

        returning: function (output, advice, context) {
            arrayForEach(_listeners, function (index, listener) {
                if (listener.returning) {
                    listener.returning(output, advice, context);
                }
            });
            arrayForEach(_regex_listeners, function (index, _regex_listener) {
                if (_regex_listener.listener.returning) {
                    _regex_listener.listener.returning(output, advice, context);
                }
            });
        },

        throwing: function (output, advice, context) {
            arrayForEach(_listeners, function (index, listener) {
                if (listener.throwing) {
                    listener.throwing(output, advice, context);
                }
            });
            arrayForEach(_regex_listeners, function (index, _regex_listener) {
                if (_regex_listener.listener.throwing) {
                    _regex_listener.listener.throwing(output, advice, context);
                }
            });
        },

    }

})

// 向全局对象注入GREYS回调函数
require(['global', 'greys'], function (global, greys) {
    global.__greys_module_test_java_class_name = function (javaClassName) {
        return greys.testJavaClassName(javaClassName);
    }
    global.__greys_module_test_java_method_name = function (javaMethodName) {
        return greys.testJavaMethodName(javaMethodName);
    }
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