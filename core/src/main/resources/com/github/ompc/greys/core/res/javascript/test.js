require({
    paths: {
        stringjs: 'https://raw.githubusercontent.com/jprichardson/string.js/master/dist/string.min.js',
        moment: 'https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.11.2/moment.min.js',
        // moment: '/tmp/moment.js',
        'text-formatting': '/Users/vlinux/IdeaProjects/github/greys-project/greys-anatomy/scripts/text-formatting-module.js',
    }
})

require(['text-formatting'], function (text) {
    var box = text.box('abcdefg');
    box.config({
        borders: ['top','bottom','left','right'],
        padding : 1,
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

