require({
    paths: {
        stringjs: 'https://raw.githubusercontent.com/jprichardson/string.js/master/dist/string.min.js',
        moment: 'https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.11.2/moment.min.js',
        // moment: '/tmp/moment.js',
        'text-formatting': '/Users/vlinux/IdeaProjects/github/greys-project/greys-anatomy/scripts/text-formatting-module.js',
        'sql-formatter': 'https://raw.githubusercontent.com/sonota88/anbt-sql-formatter.js/master/anbt-sql-formatter.js',
        'echo' : '/tmp/echo.js'
    }
})

require(['text-formatting'], function (text) {
    var box = text.box('abcdefg');
    box.config({
        borders: ['top', 'bottom', 'left', 'right'],
        padding: 1,
    });
    java.lang.System.out.println(box.rendering());
})

require(['text-formatting'], function (text) {
    var table = new text.table();
    table.config({
        borders: ['top', 'bottom', 'left', 'right', 'vertical', 'horizontal'],
        padding: 1,
        columns: [
            {
                width: 10,
                vertical: 'middle',
                horizontal: 'left'
            },
            {
                width: 20,
                vertical: 'top',
                horizontal: 'left'
            },
            {
                width: 17,
                vertical: 'middle',
                horizontal: 'left'
            }
        ]
    });

    table.row('abcdefghijklmnopqrstabcdefghijklmnox', '12345678901234567890', '!@#$%^&*()_++_)(*&^%$#@!!@#$%^&*()_++_)(*&^%$#@!');
    table.row('abcdefghijklmnopqrst', '12345678901234567890', '!@#$%^&*()_++_)(*&^%$#@!!@#$%^&*()_++_)(*&^%$#@!');
    table.row('abcdefghijklmnopqrstabcdefghijklmnox', '12345678901234567890', '!@#$%^&*()_++_)(*&^%$#@!!@#$%^&*()_++_)(*&^%$#@!');
    table.row('abcdefghijklmnopqrst', '12345678901234567890', '!@#$%^&*()_++_)(*&^%$#@!!@#$%^&*()_++_)(*&^%$#@!');
    table.row('abcdefghijklmnopqrstabcdefghijklmnox', '12345678901234567890', '!@#$%^&*()_++_)(*&^%$#@!!@#$%^&*()_++_)(*&^%$#@!');
    table.row('abcdefghijklmnopqrst', '12345678901234567890', '!@#$%^&*()_++_)(*&^%$#@!!@#$%^&*()_++_)(*&^%$#@!');
    table.row('abcdefghijklmnopqrstabcdefghijklmnox', '12345678901234567890', '!@#$%^&*()_++_)(*&^%$#@!!@#$%^&*()_++_)(*&^%$#@!');
    table.row('abcdefghijklmnopqrst', '12345678901234567890', '!@#$%^&*()_++_)(*&^%$#@!!@#$%^&*()_++_)(*&^%$#@!');


    java.lang.System.out.println(table.rendering());

})

require(['sql-formatter'], function (asf) {
    var rule = new asf.anbtSqlFormatter.Rule();
    rule.functionNames.push("DATE");
    rule.kw_minus1_indent_nl_kw_plus1_indent.push("LIMIT");
    var formatter = new asf.anbtSqlFormatter.Formatter(rule);
    java.lang.System.out.println(formatter.format('select `lg_order_goods`.`ORDER_GOODS_ID`,`lg_order_goods`.`ORDER_ID`,`lg_order_goods`.`GOODS_NAME`,`lg_order_goods`.`GOODS_PIC_ID`,`lg_order_goods`.`GOODS_QUANTITY`,`lg_order_goods`.`SELL_PROPERTY`,`lg_order_goods`.`PACKAGE_NAME`,`lg_order_goods`.`GOODS_STATUS`,`lg_order_goods`.`AUCTION_CODE`,`lg_order_goods`.`GMT_CREATE`,`lg_order_goods`.`GMT_MODIFIED`,`lg_order_goods`.`ITEM_VALUE`,`lg_order_goods`.`TRADE_ID`,`lg_order_goods`.`USER_ID`,`lg_order_goods`.`PICKING_ID`,`lg_order_goods`.`FEATURE`,`lg_order_goods`.`SYNC_VERSION` from `lg_order_goods_0838` `lg_order_goods` where ((`lg_order_goods`.`USER_ID` = ?) AND (`lg_order_goods`.`ORDER_ID` = ?)) limit ?,?'));
})

require(['echo'],function(echo){
    java.lang.System.out.println(echo.echo('hello'));
});

