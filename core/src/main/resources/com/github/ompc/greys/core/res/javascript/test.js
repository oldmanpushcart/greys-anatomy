require({
    paths: {
        adder: '/tmp/temp.js',
        stringjs: 'https://raw.githubusercontent.com/jprichardson/string.js/master/dist/string.min.js',
        moment: 'https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.11.2/moment.min.js'
    }
})

//define('global', function () {
//    return (function () {
//        return this;
//    })()
//})
//
//require(['global'], function (global) {
//    global.echo = function (str) {
//        return "echo:" + str;
//    }
//})
//
//require(['adder', 'global', 'xxx'], function (adder, global, xxx) {
//    java.lang.System.out.println(adder.addx(1, 2, 3));
//    java.lang.System.out.println(global.echo('hello'));
//    java.lang.System.out.println("" + xxx);
//})
//
//define(function (require, exports, module) {
//    java.lang.System.out.println(""+exports);
//})

//require(['stringjs'],function(stringjs){
//    java.lang.System.out.println(""+stringjs);
//})

//var regex = new RegExp("(?!)^https://.*");
//java.lang.System.out.println("regex="+/^(https{0,1}|ftp):\/\/.*/.test('httpss://cdn.rawgit.com/jprichardson/string.js/master/lib/string.min.js'));

require(['moment'], function () {
    java.lang.System.out.println(moment);
})