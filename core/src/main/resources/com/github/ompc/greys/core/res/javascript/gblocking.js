/*
 * GREYS-BLOCKING.js
 *
 * Copyright (c) 2011-2015 oldmanpushcart@gmail.com
 * License: GNU General Public License 3 (GPLv3)
 * Read more at http://shjs.sourceforge.net/doc/gplv3.html
 *
 * @author : oldmanpushcart@gmail.com
 * @date   : 2016-02-18
 *
 *
 * gblocking.js的名字含义是"greys-blocking",这是一个小型\精简的CommonJS规范的实现者
 * 该框架定义了两个核心方法: require/define
 *
 * require:
 *      require(config)
 *      require(id)
 *      require(factory)
 *      require(id,factory)
 *      require(id,dependencies,factory)
 *
 * define:
 *      define(factory)
 *      define(id,factory)
 *      define(dependencies,factory)
 *      define(id,dependencies,factory)
 *
 *
 * 叫blocking做名字主要是区分于AMD规范中的异步加载,在CommonJS规范中,其实并没有要求一定要异步加载,同步也是能满足要求.
 */

/**
 * 全局配置对象
 * @type {{util: {readTextFully}, module_mapping: {}, path_mapping: {}, config: __greys_requirejs.config}}
 * @private
 */
__greys_requirejs = this.__greys_requirejs || {

        // 工具函数集合
        util: function () {

            // 导入相关包
            var File = java.io.File;
            var FileInputStream = java.io.FileInputStream;
            var InputStream = java.io.InputStream;
            var OutputStream = java.io.OutputStream;
            var URL = java.net.URL;
            var Charset = java.nio.charset.Charset;
            var Scanner = java.util.Scanner;

            /**
             * 打开输入流
             * @param path 资源路径
             * @return 资源输入流
             */
            function openStream(path) {

                // 路径是字符串
                if (typeof(path) == "string") {

                    // HTTP/HTTPS
                    if (/^(https{0,1}):\/\/.*/.test(path.toLowerCase())) {
                        return new URL(path).openStream();
                    }

                    // 其他情况当本地文件处理
                    else {
                        return new FileInputStream(new File(path));
                    }
                }

                // 路径是输入流
                else if (path instanceof InputStream) {
                    return path;
                }

                // 输入路径是URL
                else if (path instanceof URL) {
                    return str.openStream();
                }

                // 输入路径是文件
                else if (path instanceof File) {
                    return new FileInputStream(path);
                }

                // 其他情况返回null
                else {
                    return null;
                }

            }

            /**
             * 关闭
             * @param stream 流
             */
            function close(stream) {

                if (!stream) {
                    return;
                }

                if (stream instanceof InputStream
                    || stream instanceof OutputStream
                    || stream instanceof Scanner) {
                    try {
                        stream.close();
                    } catch (e) {
                        //
                    }
                }

            }

            /**
             * 获取文本内容
             * @param path      资源路径
             * @param charset   字符编码
             * @return 文本内容
             */
            function readTextFully(path, charset) {

                var input = openStream(path);

                // 修正charset
                charset = (charset && Charset.isSupported(charset))
                    ? charset
                    : Charset.defaultCharset().name();

                var scanner = new Scanner(input, charset);
                try {
                    var content = "";
                    while (scanner.hasNextLine()) {
                        content += scanner.nextLine() + "\n";
                    }
                    return content;
                } catch (e) {
                    throw e;
                } finally {
                    close(scanner);
                }

            }

            return {
                readTextFully: readTextFully
            }
        }(),

        // ID:模块映射
        module_mapping: {},

        // ID:路径映射
        path_mapping: {},

        // 配置
        config: function (cfg) {
            if (!cfg || !cfg.paths) {
                return;
            }
            for (var prop in cfg.paths) {
                this.path_mapping[prop] = cfg.paths[prop];
            }
        }

    }

/**
 * 加载远程资源函数
 * require(id)
 * require(dependencies,function)
 * @type {Function}
 * @private
 */
__greys_require = this.__greys_require || function () {

        /**
         * 获取&加载模块
         * @param id  ID
         * @returns 模块单例
         */
        function getOrLoadModuleIfNecessary(id) {
            var module = __greys_requirejs.module_mapping[id];
            if (!module) {
                var path = __greys_requirejs.path_mapping[id];
                if (path) {
                    require(['__greys_require_remote_module_loader'], function (loader) {
                        loader(id, path);
                    })
                    // 因为loader模块中应该完成了模块的注册,所以这里直接取一次
                    module = __greys_requirejs.module_mapping[id];
                }
            }

            // exports和module这两个模块要特殊处理
            if (/^(exports|module)$/.test(id)) {
                module = module.make();
            }

            return module;
        }

        // require(id)
        if (arguments.length == 1
            && typeof(arguments[0]) == 'string') {

            var id = arguments[0];
            var module = __greys_requirejs.module_mapping[id];
            if (!module) {
                module = getOrLoadModuleIfNecessary(id);
            }

            return module;

        }

        // require(config)
        else if (arguments.length == 1
            && typeof(arguments[0]) == 'object') {
            var config = arguments[0];
            __greys_requirejs.config(config);
            return this;
        }

        // require(dependencies, func)
        else if (arguments.length == 2
            && arguments[0] instanceof Array
            && arguments[1] instanceof Function) {
            var dependencies = arguments[0];
            var func = arguments[1];

            if (dependencies
                && func) {
                var funcArguments = [];
                for (var index in dependencies) {
                    var id = dependencies[index];
                    if (typeof(id) == 'string') {
                        funcArguments[index] = getOrLoadModuleIfNecessary(id);
                    } else {
                        funcArguments[index] = null;
                    }
                }
                func.apply(this, funcArguments);
            }

            return this;
        }

        return this;
    }

/**
 * 模块定义函数
 * define(id,dependencies,factory)
 * define(dependencies,factory)
 * define(factory)
 * @type {Function}
 * @private
 */
__greys_define = this.__greys_define || function () {

        var default_dependencies = ['require', 'exports', 'module'];
        var empty_dependencies = [];

        // define(factory)
        if (arguments.length == 1
            && arguments[0] instanceof Function) {
            return this.__greys_define("", default_dependencies, arguments[0]);
        }

        // define(dependencies,factory)
        else if (arguments.length == 2
            && arguments[0] instanceof Array
            && arguments[1] instanceof Function) {
            return this.__greys_define("", arguments[0], arguments[1]);
        }

        // define(id,factory)
        else if (arguments.length == 2
            && typeof(arguments[0]) == 'string'
            && arguments[1] instanceof Function) {
            return this.__greys_define(arguments[0], empty_dependencies, arguments[1]);
        }

        // define(id,dependencies,factory)
        else if (arguments.length == 3
            && typeof(arguments[0]) == 'string'
            && arguments[1] instanceof Array
            && arguments[2] instanceof Function) {

            var id = arguments[0];
            var dependencies = arguments[1];
            var factory = arguments[2];

            var module = null;
            __greys_require(dependencies, function () {

                // 构造魔法模块export,module
                var exportsModule, moduleModule;
                for (var index in dependencies) {
                    if (dependencies[index] == 'exports') {
                        exportsModule = arguments[index];
                    } else if (dependencies[index] == 'module') {
                        moduleModule = arguments[index];
                    }//if
                }//for
                if (exportsModule && moduleModule) {
                    moduleModule.exports = exportsModule;
                }

                // 构造模块
                module = factory.apply(this, arguments);

                // 只有factory函数没有主动返回时才需要动用魔法模块
                if (!module) {
                    // 应用魔法模块
                    if (exportsModule) {
                        module = exportsModule;
                    }
                    if (moduleModule) {
                        if (moduleModule.exports != exportsModule) {
                            module = moduleModule.exports;
                        }
                        if (moduleModule.id) {
                            id = moduleModule.id;
                        }
                    }
                }

                // 模块定义声明了id,需要主动注册到模块集合中
                if (id.length != 0) {
                    __greys_requirejs.module_mapping[id] = module;
                }

            });

            return module;

        }

        // define()
        else {
            return;
        }

    }


/**
 * 加载JS资源
 * @type {Function}
 */
__greys_load = this.__greys_load || function () {

        // load(path)
        if (arguments.length == 1) {
            this.__greys_load(arguments[0], null);
        }

        // load(path,charset)
        else if (arguments.length == 2) {
            var path = arguments[0];
            var charset = arguments[1];
            eval(__greys_requirejs.util.readTextFully(path, charset));
        }

        // none
        else {
            return;
        }

    }


// 初始化load函数
load = this.load || this.__greys_load;


// 初始化require
if (!this.hasOwnProperty('require')
    && !this.hasOwnProperty('define')) {
    this.require = __greys_require;
    this.define = __greys_define;
}

// ------------------------- MAGIC MODULE 定义 -------------------------
// 一共定义了五个MAGIC MODULE
// require/exports/module 这三个模块主要是兼容CommonJS开发规范
// global模块主要是用于获取全局this对象,一些函数可能需要挂在全局this
// __greys_require_remote_module_loader模块主要用于内部实现加载远程模块

__greys_define('require', [], function () {
    return __greys_require;
})

__greys_define('exports', [], function () {
    return {
        make: function () {
            return {}
        }
    }
})

__greys_define('module', [], function () {
    return {
        make: function () {
            return {}
        }
    }
})

/**
 * 定义全局对象模块
 * global == window
 */
__greys_define('global', function () {
    return (function () {
        return this;
    })()
})

/**
 * 远程模块加载模块
 */
__greys_define('__greys_require_remote_module_loader', function () {
    return function (id, path) {
        __greys_define(function (require, exports, module) {
            eval(__greys_requirejs.util.readTextFully(path));
            var moduleModule = (module.exports && module.exports != exports) ? module.exports : exports;
            if (module.id) {
                __greys_requirejs.module_mapping[module.id] = moduleModule;
            }
            __greys_requirejs.module_mapping[id] = moduleModule;
        })
    }
})

