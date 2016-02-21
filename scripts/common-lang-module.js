/**
 * 通用模块
 * @type {{}}
 */

/**
 * 数组操作通用模块
 */
define('__common_lang_array', function () {

    function arrayLength(array) {
        return array ? array.length : 0;
    }

    return {

        /**
         * 遍历
         * @param array    数组
         * @param callback 回调函数
         *                 function(index, element)
         */
        forEach: function (array, callback) {
            if (array) {
                for (var index in array) {
                    callback(index, array[index]);
                }
            }
        },

        /**
         * 数组大小
         * @param array 数组
         * @returns {number}
         */
        length: arrayLength,

        /**
         * 数组是否为空
         * @param array 数组
         * @returns {boolean}
         */
        isEmpty: function (array) {
            return length(array) <= 0;
        },

        isIn: function (array, element) {
            var it = new this.iterator(array);
            while (it.hasNext()) {
                var e = it.next();
                if (e == element) {
                    return true;
                }
            }//while
            return false;
        },

        iterator: function (array) {

            var _array = array;
            var _pos = 0;

            return {

                hasNext: function () {
                    return _pos + 1 < arrayLength(_array);
                },

                next: function () {
                    if (!this.hasNext()) {
                        throw 'no more elements!';
                    }
                    return _array[++_pos];
                },

                remove: function () {
                    var element = _array[_pos];
                    _array.splice(_pos, 1);
                    return element;
                }

            }
        },

        /**
         * 判断数组中是否包含指定元素
         * @param array  数组
         * @param target 指定元素
         * @returns {boolean}
         */
        contains: function (array, target) {
            var it = new this.iterator(array);
            while (it.hasNext()) {
                if (it.next() == target) {
                    return true;
                }
            }
            return false;
        }

    }
})

define('__common_lang_string', function () {

    function stringLength(string) {
        return string ? string.length : 0;
    }

    function stringIsBlank(string) {
        return !string || /^\s*$/.test(string);
    }

    function stringForEach(string, callback) {
        if (string) {
            for (var index = 0; index < string.length; index++) {
                callback(index, string[index]);
            }
        }
    }

    return {

        equals: function (string1, string2) {
            return string1 == string2;
        },

        /**
         * 遍历
         * @param string   字符串
         * @param callback 回调函数
         *                 function(index, element)
         */
        forEach: stringForEach,

        /**
         * 字符串长度
         * @param string 字符串
         * @returns {number}
         */
        length: stringLength,

        /**
         * 判断字符串是否为空
         * @param string 字符串
         * @returns {boolean}
         */
        isEmpty: function (string) {
            return stringLength(string) <= 0;
        },

        /**
         * 判断字符串是否为空白字符串
         * @param string 字符串
         * @returns {boolean}
         */
        isBlank: stringIsBlank,

        /**
         * 判断字符串是否为非空白字符串
         * @param string 字符串
         * @returns {boolean}
         */
        isNotBlank: function (string) {
            return !stringIsBlank(string);
        },


        /**
         * 字符串重复复制
         * @param string  待重复复制字符串
         * @param repeat  重复次数
         * @returns {string}
         */
        repeat: function (string, repeat) {
            var repeatString = "";
            for (var i = 0; i < repeat; i++) {
                repeatString += string;
            }
            return repeatString;
        },

        /**
         * 字符串按指定宽度换行
         * @param string 字符串
         * @param width  换行宽度
         * @returns {string}
         */
        wrap: function (string, width) {
            var wrapString = "";
            var count = 0;
            stringForEach(string, function (index, c) {
                if (count == width) {
                    count = 0;
                    wrapString += '\n';
                    if (c == '\n') {
                        return;
                    }
                }
                if (c == '\n') {
                    count = 0;
                } else {
                    count++;
                }
                wrapString += c;
            });
            return wrapString;
        },

    }
})


define('__common_lang_text', ['__common_lang_string'], function (string) {

    /**
     * 文本矩阵宽度
     * @param text 文本矩阵
     * @returns {number}
     */
    function textWidth(text) {
        var contentWidth = count = 0;
        string.forEach(text, function (index, c) {
            if (c == '\n') {
                contentWidth = count > contentWidth ? count : contentWidth;
                count = 0;
            } else {
                count++;
            }
        });
        // 如果字符串没有一个换行符,则取当前字符串长度即可
        return contentWidth == 0 ? string.length(text) : contentWidth;
    }

    /**
     * 文本矩阵高度
     * @param text 文本矩阵
     * @returns {number}
     */
    function textHeight(text) {

        // 初始高度
        var contentHeight = 0;

        // 遍历内容
        string.forEach(text, function (index, c) {
            // 因为内容可能没有主动换行符,所以只要第一次字符出现则高度即为1
            // 随后每次出现一个换行符,则都判定为高度+1
            if (contentHeight == 0
                || c == '\n') {
                contentHeight++;
            }
        });
        return contentHeight;
    }

    /**
     * 文本矩阵左插入
     * @param text   文本矩阵
     * @param insert 待插入字符串
     * @returns {*}
     */
    function textLeftInsert(text, insert) {
        var content = insert;
        string.forEach(text, function (index, c) {
            content += c;
            if (c == '\n') {
                content += insert;
            }
        });
        return content;
    }

    function stringBlank(length) {
        return string.repeat(' ', length);
    }

    /**
     * 文本矩阵右插入
     * @param text   文本矩阵
     * @param insert 待插入字符串
     * @param width  横跨宽度
     * @returns {*}
     */
    function textRightInsert(text, insert, width) {
        width = Math.max(width, textWidth(text));
        var content = "";
        var count = 0;
        string.forEach(text, function (index, c) {
            count++;
            if (c == '\n') {
                content += (stringBlank(width - count) + insert);
                count = 0;
            }
            content += c;
            // 如果当前遍历到最后一个字符,则需要修正计数器++
            // 否则会在最后计算偏移量的时候多计算一个字符
            if (index == string.length(text) - 1) {
                count++;
            }
        });

        return content + (stringBlank(width - count) + insert);
    }

    /**
     * 文本矩阵整体右移
     * 整体向右移动shift个字符
     * @param text   文本矩阵
     * @param shift  移动量
     * @returns {*}
     */
    function textRightShift(text, shift) {
        return textLeftInsert(text, stringBlank(shift));
    }

    /**
     * 文本矩阵整体下移
     * 整体下移shift个字符
     * @param text   文本矩阵
     * @param shift  移动量
     * @returns {*}
     */
    function textBottomShift(text, shift) {
        for (var i = 0; i < shift; i++) {
            text = '\n' + text;
        }
        return text;
    }

    return {
        width: textWidth,
        height: textHeight,
        insertLeft: textLeftInsert,
        insertRight: textRightInsert,
        shiftRight: textRightShift,
        shiftBottom: textBottomShift,
    }

})

define('__common_lang_java', ['__common_lang_string', '__common_lang_array'], function (string, array) {

    /**
     * 获取Java对象的类名
     * @param javaObject java对象
     * @returns {string}
     */
    function className(javaObject) {
        return javaObject ? javaObject.getClass().getName() : null;
    }

    /**
     * 反射获取Java对象属性值
     * @param javaObject Java对象
     * @param fieldName  属性名称
     * @returns {*}
     */
    function reflectGetFieldValue(javaObject, fieldName) {
        var field = javaObject.getClass().getDeclaredField(fieldName);
        var isAccessible = field.isAccessible();
        try {
            field.setAccessible(true);
            return field.get(javaObject);
        } finally {
            field.setAccessible(isAccessible);
        }
    }

    /**
     * 反射判断Java对象是否拥有指定属性
     * @param javaObject Java对象
     * @param fieldName  属性名称
     * @returns {boolean}
     */
    function reflectHasField(javaObject, fieldName) {
        var it = array.iterator(javaObject.getClass().getDeclaredFields());
        while (it.hasNext()) {
            var field = it.next();
            if (string.equals(field.getName(), fieldName)) {
                return true;
            }
        }
        return false;
    }

    return {

        /**
         * 属性操作函数集合
         */
        field: {

            /**
             * 获取属性值
             */
            get: reflectGetFieldValue,

            /**
             * 是否拥有属性
             */
            has: reflectHasField,

        },

        /**
         * 类操作函数结合
         */
        clazz: {

            /**
             * 获取Java对象的类名
             * @param javaObject java对象
             * @returns {string}
             */
            name: function (javaObject) {
                return javaObject ? javaObject.getClass().getName() : null;
            },

            isJava: function (javaObject) {
                return javaObject ? javaObject instanceof java.lang.Object : null;
            },

        },

    }

})

require(
    ['__common_lang_string', '__common_lang_array', '__common_lang_text', '__common_lang_java'],
    function (string, array, text, java) {
        module.exports = {
            array: array,
            string: string,
            text: text,
            java: java,
        }
    })



