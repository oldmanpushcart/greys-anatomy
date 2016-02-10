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

var __global_greys;
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


