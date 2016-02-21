/**
 * stream-statistics-module.js是一个流式统计库,推崇用时间来换空间
 * 统计库本身不会存储所有纳入统计范围的数据,而是实时计算每一次的结果,会消耗CPU资源,但却能极大节省存储空间
 * @type {{MIN: module.exports.MIN, MAX: module.exports.MAX, AVG: module.exports.AVG, SUM: module.exports.SUM, COUNT: module.exports.COUNT, create: module.exports.create}}
 */

module.exports = {

    /**
     * 最小值
     */
    MIN: function (stat, column, number) {
        var data = stat.stats()[column];
        stat.stats()[column] = data ? Math.min(data, number) : number;
    },

    /**
     * 最大值
     */
    MAX: function (stat, column, number) {
        var data = stat.stats()[column];
        stat.stats()[column] = data ? Math.max(data, number) : number;
    },

    /**
     * 平均值
     */
    AVG: function (stat, column, number) {
        stat.stats()[column] = stat.sums()[column] / stat.counts()[column];
    },

    /**
     * 求和
     */
    SUM: function (stat, column, number) {
        stat.stats()[column] = stat.sums()[column];
    },

    /**
     * 计数
     */
    COUNT: function (stat, column, number) {
        var data = stat.stats()[column];
        stat.stats()[column] = data ? data + 1 : 1;
    },

    /**
     * 构造统计器
     * @returns {{config: config, stats: stats, sums: sums, counts: counts, reset: reset}}
     */
    create: function () {

        // 配置信息
        var _config = [];

        // 每一列的计数
        var _col_counts = [];

        // 每一列之和
        var _col_sums = [];

        // 统计数据
        var _stats = [];

        // init
        if (arguments.length == 1
            && arguments[0] instanceof Array) {
            config(arguments[0]);
        }

        function config(config) {
            _config = config;
        }

        /**
         * 判断是否为数字(null不是数字)
         * @param n
         * @returns {boolean}
         */
        function isNumber(n) {
            if (n == null) {
                return false;
            }
            return !isNaN(n);
        }

        /**
         * 列计数器++
         * @param col 列
         */
        function incColCount(col) {
            var count = _col_counts[col];
            if (count) {
                _col_counts[col] = count + 1;
            } else {
                _col_counts[col] = 1;
            }
        }

        /**
         * 计算列之和
         * @param col 列
         * @param n   数字
         */
        function computeColSum(col, n) {
            var sum = _col_sums[col];
            if (sum != null) {
                _col_sums[col] += n;
            } else {
                _col_sums[col] = n;
            }
        }

        /**
         * 进入统计
         * stat(data1,data2,data3,...)
         *
         * 获取统计信息
         * stat()
         */
        function stats() {

            // stat()
            if (arguments.length == 0) {
                return _stats;
            }


            // stat(data1,data2,data3,...)
            for (var column = 0; column < arguments.length; column++) {

                // 超过列定义的数据不纳入统计
                if (column >= _config.length) {
                    break;
                }

                var argument = arguments[column];

                // 非数字的不纳入统计
                if (!isNumber(argument)) {
                    continue;
                }

                // 列计数
                incColCount(column);

                // 列计和
                computeColSum(column, argument);

                // 执行统计函数
                _config[column](this, column, argument);

            }

        }

        /**
         * 重置统计数据
         */
        function reset() {
            _stats = [];
            _col_counts = [];
            _col_sums = [];
        }

        return {
            /**
             * 获取统计配置信息
             * @returns {Array}
             */
            config: function () {
                return _config;
            },

            /**
             * 纳入统计
             */
            stats: stats,

            /**
             * 获取每一列:之和
             * @returns {Array}
             */
            sums: function () {
                return _col_sums;
            },

            /**
             * 获取每一列:计数
             * @returns {Array}
             */
            counts: function () {
                return _col_counts;
            },

            /**
             * 统计重置
             */
            reset: reset,

        }
    }

}


