/*
 * JavaScriptLogger
 * 这是一个JavaScript脚本支撑的样板工程,方法的执行日志
 * @author : oldmanpushcart@gmail.com
 */


/**
 * 对Date的扩展，将 Date 转化为指定格式的String
 * 月(M)、日(d)、小时(h)、分(m)、秒(s)、季度(q) 可以用 1-2 个占位符，
 * 年(y)可以用 1-4 个占位符，毫秒(S)只能用 1 个占位符(是 1-3 位的数字)
 * 例子：
 * (new Date()).Format("yyyy-MM-dd hh:mm:ss.S") ==> 2006-07-02 08:09:04.423
 * (new Date()).Format("yyyy-M-d h:m:s.S")      ==> 2006-7-2 8:9:4.18
 * @param fmt 日期格式
 * @returns 格式化后的日期字符串
 */
Date.prototype.format = function (fmt) { //author: meizz
    var o = {
        "M+": this.getMonth() + 1, //月份
        "d+": this.getDate(), //日
        "h+": this.getHours(), //小时
        "m+": this.getMinutes(), //分
        "s+": this.getSeconds(), //秒
        "q+": Math.floor((this.getMonth() + 3) / 3), //季度
        "S": this.getMilliseconds() //毫秒
    };
    if (/(y+)/.test(fmt)) fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
    for (var k in o)
        if (new RegExp("(" + k + ")").test(fmt)) fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
    return fmt;
}

/**
 * 字符串格式化函数
 * var template1="我是{0}，今年{1}了";
 * var template2="我是{name}，今年{age}了";
 * var result1=template1.format("loogn",22);
 * var result2=template1.format({name:"loogn",age:22});
 * @param args 参数列表
 * @returns 格式化后的字符串
 */
String.prototype.format = function (args) {
    if (arguments.length > 0) {
        var result = this;
        if (arguments.length == 1 && typeof (args) == "object") {
            for (var key in args) {
                var reg = new RegExp("({" + key + "})", "g");
                result = result.replace(reg, args[key]);
            }
        }
        else {
            for (var i = 0; i < arguments.length; i++) {
                if (arguments[i] == undefined) {
                    return "";
                }
                else {
                    var reg = new RegExp("({[" + i + "]})", "g");
                    result = result.replace(reg, arguments[i]);
                }
            }
        }
        return result;
    }
    else {
        return this;
    }
}

/**
 * 获取当前系统时间戳
 * @returns 时间戳字符串
 */
function timestamp() {
    return new Date().format("yyyy-MM-dd hh:mm:ss.S");
}

/**
 * 日志前缀
 * @param advice Advice
 * @return 日志前缀内容
 */
function prefix(advice) {
    return "{timestamp} {classname} {methodname}".format({
        'timestamp': timestamp(),
        'classname': advice.clazz.name,
        'methodname': advice.method.name
    });
}

/**
 * 输出Java的Throwable异常信息
 * @param throwing Java异常信息
 * @return 异常信息字符串堆栈
 */
function printingJavaThrowable(throwing) {
    var throwingString = null;
    var sw = new java.io.StringWriter();
    var pw = new java.io.PrintWriter(sw);
    try {
        throwing.printStackTrace(pw);
        throwingString = sw.toString();
    } finally {
        pw.close();
        sw.close();
    }
    return throwingString;
}

function returning(output, advice, context) {
    finish(output, advice, context);
}

function throwing(output, advice, context) {
    finish(output, advice, context);
}

function finish(output, advice, context) {
    var content = "{0} : cost={1}ms;".format(prefix(advice), context.cost);

    // 拼装参数列表
    if (advice.params.length > 0) {
        content += "params[{0}];".format(function () {
            var paramString = "";
            for (var index in advice.params) {
                paramString += advice.params[index];
                if (index < advice.params.length - 1) {
                    paramString += ",";
                }
            }
            return paramString;
        });
    }

    // 拼装返回值
    if (advice.isReturning) {
        content += "return[{0}];".format(advice.returnObj);
    }

    // 拼装异常信息
    if (advice.isThrowing) {
        content += "throwing[{throwing}];".format({'throwing': advice.throwExp});
        content += "\n" + printingJavaThrowable(advice.throwExp);
    }

    output.println(content);
}