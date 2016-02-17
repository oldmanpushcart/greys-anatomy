/**
 * 这里声明和定义了greys对JavaScript的支持函数
 * @author vlinux
 */

///**
// * 因为Nashorn在支持require-js的时候存在BUG,缺少了readFully函数
// * 所以在这里进行定义
// *
// * 完整的URL的文本内容
// * @param URL
// * @return 文本内容
// * @type {readFully|Function}
// */
//readFully = this.readFully || (function (URL) {
//
//        var content = "";
//        var reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(inStream(URL))));
//        try {
//            var line;
//            do {
//                line = reader.readLine();
//                content += line + "\n";
//            } while (line != null);
//        } finally {
//            reader.close();
//        }
//
//        return content;
//    });




/**
 * GREYS全局函数
 * @type {Function}
 */
__define_golbal_greys = this.__define_golbal_greys || (function (__GREYS_JS_TARGET) {

        var before, create, destroy, returning, throwing;
        eval(__GREYS_JS_TARGET);
        return {
            isDefineCreate: function () {
                return create != null;
            },
            isDefineDestroy: function () {
                return destroy != null;
            },
            isDefineBefore: function () {
                return before != null;
            },
            isDefineReturning: function () {
                return returning != null;
            },
            isDefineThrowing: function () {
                return throwing != null;
            },
            create: function (output) {
                create && create(output);
            },
            destroy: function (output) {
                destroy && destroy(output);
            },
            before: function (output, advice, context) {
                return before && before(output, advice, context);
            },
            returning: function (output, advice, context) {
                returning && returning(output, advice, context);
            },
            throwing: function (output, advice, context) {
                throwing && throwing(output, advice, context);
            }
        }

    });

// GREYS全局变量
var __global_greys;

/**
 * GREYS全局变量初始化
 * @param target 目标启动JavaScript脚本内容(也就需要加载执行的脚本内容)
 */
function __global_greys_init(target) {
    __global_greys = new __define_golbal_greys(target);
}

function __global_greys_create(output) {
    return __global_greys.create(output);
}

function __global_greys_destroy(output) {
    return __global_greys.destroy(output);
}

function __global_greys_before(output, advice, context) {
    return __global_greys.before(output, advice, context);
}

function __global_greys_returning(output, advice, context) {
    return __global_greys.returning(output, advice, context);
}

function __global_greys_throwing(output, advice, context) {
    return __global_greys.throwing(output, advice, context);
}

function __global_greys_is_define_create() {
    return __global_greys.isDefineCreate();
}

function __global_greys_is_define_destroy() {
    return __global_greys.isDefineDestroy();
}

function __global_greys_is_define_before() {
    return __global_greys.isDefineBefore();
}

function __global_greys_is_define_returning() {
    return __global_greys.isDefineReturning();
}

function __global_greys_is_define_throwing() {
    return __global_greys.isDefineThrowing();
}


