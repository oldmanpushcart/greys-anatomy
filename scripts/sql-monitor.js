__greys_require({
    paths: {
        tui: 'https://raw.githubusercontent.com/oldmanpushcart/greys-javascript-lib/master/script/lib/text-formatting-module.js',
        stats: 'https://raw.githubusercontent.com/oldmanpushcart/greys-javascript-lib/master/script/lib/stream-statistics-module.js',
        lang: 'https://raw.githubusercontent.com/oldmanpushcart/greys-javascript-lib/master/script/lib/common-lang-module.js',
        scheduler: 'https://raw.githubusercontent.com/oldmanpushcart/greys-javascript-lib/master/script/lib/scheduler-module.js',
    }
})

/**
 * 模版
 */
__greys_require(['greys', 'lang', 'tui', 'stats', 'scheduler'], function (greys, lang, tui, stats, scheduler) {

    // 监控数据(K(id):V(stats,sql))
    var monitor = {};
    var timer;
    var lock = new lang.lock();

    // Statement解析器
    var parsers = [

        // mysql-jdbc-connector-parser on execute()
        {
            test: function (advice) {
                return /^com\.mysql\.jdbc\.PreparedStatement$/.test(advice.clazz.name)
                    && /^(execute|executeQuery|executeUpdate|executeBatch|executeLargeBatch|executeLargeUpdate)$/.test(advice.method.name)
            },

            parse: function (advice) {
                if (lang.array.isEmpty(advice.params)) {
                    return advice.target.getNonRewrittenSql();
                }

                else if (advice.params[0]
                    && lang.string.equals(lang.java.clazz.name(advice.params[0]), 'java.lang.String')
                    && lang.string.isNotBlank("" + advice.params[0])) {
                    return "" + advice.params[0];
                }

                else {
                    return "UNKNOW-SQL";
                }
            }
        },

    ];

    function _create(output) {
        timer = new scheduler();
        timer.setInterval(function () {

            var _monitor;
            lock.lock();
            try {
                _monitor = monitor;
                monitor = {};
            } finally {
                lock.unlock();
            }

            var table = new tui.table();
            table.config({
                borders: ['top', 'bottom', 'left', 'right', 'vertical', 'horizontal'],
                padding: 1,
                columns: [
                    {
                        width: 22,
                        vertical: 'middle',
                        horizontal: 'left'
                    },
                    {
                        width: 80,
                        vertical: 'middle',
                        horizontal: 'left'
                    },
                    {
                        width: 10,
                        vertical: 'middle',
                        horizontal: 'left'
                    },
                    {
                        width: 10,
                        vertical: 'middle',
                        horizontal: 'left'
                    },
                    {
                        width: 10,
                        vertical: 'middle',
                        horizontal: 'left'
                    },
                    {
                        width: 10,
                        vertical: 'middle',
                        horizontal: 'left'
                    },
                    {
                        width: 10,
                        vertical: 'middle',
                        horizontal: 'left'
                    },
                    {
                        width: 10,
                        vertical: 'middle',
                        horizontal: 'left'
                    }
                ]
            });

            // add title
            table.row('TIMESTAMP', 'SQL', 'TOTAL', 'SUCCESS', 'FAIL', 'RT', 'RT-MIN', 'RT-MAX');
            var timestamp = lang.date.format(new Date(), 'yyyy-MM-dd hh:mm:ss');

            for (var sql in _monitor) {
                var stat = _monitor[sql];
                var report = stat.stats();
                table.row(
                    "" + timestamp,
                    "" + sql,
                    "" + report[0],
                    "" + report[1],
                    "" + report[2],
                    "" + (report[3].toFixed(2)),
                    "" + (report[4].toFixed(2)),
                    "" + (report[5].toFixed(2))
                );
            }

            output.println(table.rendering());

        }, 1000 * 60);
    }

    function _destroy() {
        timer.shutdown();
    }

    function getParser(advice) {
        var it = lang.array.iterator(parsers);
        while (it.hasNext()) {
            var parse = it.next();
            if (parse.test(advice)) {
                return parse;
            }
        }
    }

    function finish(output, advice, context) {

        var parse = getParser(advice);
        if (!parse) {
            return;
        }

        var sql = parse.parse(advice);
        if (!sql) {
            return;
        }

        lock.lock();
        try {
            var stat = monitor[sql];
            if (!stat) {
                stat = monitor[sql] = stats.create([
                    stats.SUM,
                    stats.SUM,
                    stats.SUM,
                    stats.AVG,
                    stats.MIN,
                    stats.MAX,
                ]);
            }

            stat.stats(
                1,
                advice.isReturning ? 1 : 0,
                advice.isThrowing ? 1 : 0,
                context.getCost(),
                context.getCost(),
                context.getCost()
            );
        } finally {
            lock.unlock();
        }

    }

    greys.watching({
        create: _create,
        destroy: _destroy,
        returning: finish,
        throwing: finish,
    });
})