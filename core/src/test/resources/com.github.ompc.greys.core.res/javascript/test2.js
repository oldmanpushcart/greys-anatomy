require({
    paths: {
        'lang': 'https://raw.githubusercontent.com/oldmanpushcart/greys-javascript-lib/master/script/lib/common-lang-module.js',
    }
})

/**
 * 定义了一个console模块
 * 简单实现,不要吐槽
 */
define('console', function () {

    function print(string) {
        java.lang.System.out.print("" + string);
    }

    function println(string) {
        print(string + '\n');
    }

    return {
        log: function (msg) {
            println(msg);
        }
    }
})


require(['lang', 'console'], function (lang, console) {

    var words = lang.string.format('{0} is dead, but {1} is alive! {0} {2}', 'ASP', 'ASP.NET');
    console.log(words);

})