/**
 * 调度器模块,可以模拟javascript的setTimeout/setInterval
 */

module.exports = function () {

    var executor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
    var counter = 1;
    var ids = {};

    function clearTimeout(id) {
        var future = ids[id];
        if (future) {
            ids[id].cancel(false);
            executor.purge();
            delete ids[id];
        }
    }

    return {

        setTimeout: function (fn, delay) {
            var id = counter++;
            ids[id] = executor.schedule(new java.lang.Runnable({run: fn}), delay, java.util.concurrent.TimeUnit.MILLISECONDS);
            return id;
        },

        clearTimeout: clearTimeout,

        setInterval: function (fn, delay) {
            var id = counter++;
            ids[id] = executor.scheduleAtFixedRate(new java.lang.Runnable({run: fn}), delay, delay, java.util.concurrent.TimeUnit.MILLISECONDS);
            return id;
        },

        clearInterval: clearTimeout,

        /**
         * 关闭调度器
         */
        shutdown: function () {
            executor.shutdown();
        }

    }

}
