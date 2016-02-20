/**
 * 判断数组中是否包含指定元素
 * @param array  数组
 * @param target 指定元素
 * @returns {boolean}
 */
function arrayContain(array, target) {
    for (var index in array) {
        if (array[index] == target) {
            return true;
        }
    }
    return false;
}

/**
 * 字符串遍历
 * @param string 字符串
 * @param fun    回调函数
 */
function stringForEach(string, fun) {
    for (var index = 0; index < string.length; index++) {
        var c = string[index];
        fun(index, c);
    }
}

/**
 * 字符串空白右填充
 * @param length 填充长度
 * @returns {string}
 */
function stringRightPadding(string, length) {
    var empty = stringBlank(Math.max(string.length, length));
    var padding = "";
    stringForEach(empty, function (index, c) {
        padding += index < string.length ? string[index] : c;
    });
    return padding;
}

/**
 * 文本矩阵宽度
 * @param text 文本矩阵
 * @returns {number}
 */
function textWidth(text) {
    var contentWidth = 0;
    var count = 0;

    stringForEach(text, function (index, c) {
        if (c == '\n') {
            contentWidth = count > contentWidth ? count : contentWidth;
            count = 0;
        } else {
            count++;
        }
    });

    // 如果字符串没有一个换行符,则取当前字符串长度即可
    return contentWidth == 0 ? text.length : contentWidth;
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
    stringForEach(text, function (index, c) {
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
 * 字符串空白填充
 * @param length 填充长度
 * @returns {string}
 */
function stringBlank(length) {
    return stringRepeat(' ', length);
}

/**
 * 文本矩阵整体右移
 * 整体向右移动shift个字符
 * @param text   文本矩阵
 * @param shift  移动量
 * @returns {*}
 */
function textRightShift(text, shift) {
    var content = stringBlank(shift);
    stringForEach(text, function (index, c) {
        content += c;
        if (c == '\n') {
            content += stringBlank(shift);
        }
    });
    return content;
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

/**
 * 文本矩阵左插入
 * @param text   文本矩阵
 * @param insert 待插入字符串
 * @returns {*}
 */
function stringLeftInsert(text, insert) {
    var content = insert;
    stringForEach(text, function (index, c) {
        content += c;
        if (c == '\n') {
            content += insert;
        }
    });
    return content;
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
    stringForEach(text, function (index, c) {
        count++;
        if (c == '\n') {
            content += (stringBlank(width - count) + insert);
            count = 0;
        }
        content += c;

        // 如果当前遍历到最后一个字符,则需要修正计数器++
        // 否则会在最后计算偏移量的时候多计算一个字符
        if (index == text.length - 1) {
            count++;
        }
    });

    return content + (stringBlank(width - count) + insert);
}

/**
 * 字符串按指定宽度换行
 * @param string
 * @param width
 * @returns {string}
 */
function stringWrap(string, width) {
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
}

/**
 * 字符串重复复制
 * @param repeat 待重复复制字符串
 * @param times  重复次数
 * @returns {string}
 */
function stringRepeat(repeat, times) {
    var string = "";
    for (var i = 0; i < times; i++) {
        string += repeat;
    }
    return string;
}

/**
 * 删除数据中指定元素
 * @param array  数组
 * @param target 指定元素
 */
function arrayRemove(array, target) {
    var tempArray = [];
    while (array.length) {
        var e = array.pop();
        if (e != target) {
            tempArray.push(e);
        }
    }
    for (var index in tempArray) {
        array.push(tempArray[index]);
    }
}

/**
 * box-formatting
 * 定义一个容器(box),容器是所有文本组件的基础
 */
__greys_define('text-box-formatting', function () {

    return function (content) {

        // 原始文本内容
        var _content = content;

        // 配置对象
        var _config = {
            borders: [],
            padding: 0,
            width: null,
            height: null,
            vertical: 'top',
            horizontal: 'left'
        };

        // 是否需要重新渲染
        var _isReRenderNecessary = true;

        // 容器内容
        var _boxContent;

        // 容器宽度(单位:字符)
        var _width;

        // 容器高度(单位:字符)
        var _height;

        // 解析配置
        function parse_config(config) {
            if (config) {
                _config.borders = config.borders || _config.borders;
                _config.padding = config.padding || _config.padding;
                _config.width = config.width || _config.width;
                _config.height = config.height || _config.height;
                _config.vertical = config.vertical || _config.vertical;
                _config.horizontal = config.horizontal || _config.horizontal;
            }
        }

        /**
         * 配置
         * @param config
         */
        function config() {

            // config()
            if (arguments.length == 0) {
                _isReRenderNecessary = true;
                return _config;
            }

            // config(config)
            else if (arguments.length == 1) {
                var config = arguments[0];
                parse_config(config);
                _isReRenderNecessary = true;
            }

        }

        /**
         * 纠正负数为0
         * 如果传入的数值为负数,则纠正为0
         * @param number 数字
         * @returns {number}
         */
        function correctNegative(number) {
            return number > 0 ? number : 0;
        }

        /**
         * 获取内容容量宽度(CapacityWidth)
         * 内容宽度 = 容器宽度 - padding * 2 - 左边框(如有:1) - 有边框(如有:1)
         * 如果没有配置宽度,则取字符串长度为宽度
         * 如果计算后内容宽度如小于0,则修正为0
         * @returns {number}
         */
        function computeCapacityWidthOfContent() {

            // 初始值如果配置了宽度,则取配置宽度,否则取字符串长度
            var contentCapacityWidth = _config.width || textWidth(_content);

            // 固定宽度需要对padding和边框宽度进行修正
            if (_config.width) {
                // 如果配置了padding,则需要减去2倍的padding(左右padding)
                if (_config.padding) {
                    contentCapacityWidth -= _config.padding * 2;
                }

                // 剪去左边框宽度
                if (arrayContain(_config.borders, 'left')) {
                    contentCapacityWidth--;
                }

                // 减去右边框宽度
                if (arrayContain(_config.borders, 'right')) {
                    contentCapacityWidth--;
                }
            }

            return correctNegative(contentCapacityWidth);
        }

        /**
         * 获取内容容量高度(CapacityHeight)
         * @param contentHeight 内容高度
         * @returns {number}
         */
        function computeCapacityHeightOfContent(contentHeight) {

            // 初始值如果配置了高度,则取配置的高度,否则取内容高度
            var contentCapacityHeight = _config.height || contentHeight;

            // 如果是固定高度,则需要根据边框高度进行修正
            if (_config.height) {
                // 减去上边框高度
                if (arrayContain(_config.borders, 'top')) {
                    contentCapacityHeight--;
                }

                // 减去下边框高度
                if (arrayContain(_config.borders, 'bottom')) {
                    contentCapacityHeight--;
                }
            }

            return correctNegative(contentCapacityHeight);
        }

        /**
         * 计算文本内容在容器中的右移量
         * @param contentCapacityWidth 内容容量宽度
         * @param contentWidth         内容宽度
         */
        function computeRightShift(contentCapacityWidth, contentWidth) {

            var horizontal = _config.horizontal;
            switch (horizontal) {
                case "left" :
                {
                    // 左对齐不需要向右移动
                    return 0;
                }
                case "right":
                {
                    // 右对齐
                    return correctNegative(contentCapacityWidth - contentWidth);
                }
                case "middle" :
                {
                    // 居中对齐
                    return parseInt(correctNegative(contentCapacityWidth - contentWidth) / 2);
                }
                default:
                {
                    return 0;
                }
            }
        }

        /**
         * 计算文本内容在容器中的下移量
         * @param contentCapacityHeight 内容容量高度
         * @param contentHeight         内容高度
         * @returns {number}
         */
        function computeBottomShift(contentCapacityHeight, contentHeight) {
            var vertical = _config.vertical;
            switch (vertical) {
                case "top" :
                {
                    // 上对齐不需要移动
                    return 0;
                }
                case "bottom":
                {
                    // 下对齐
                    return correctNegative(contentCapacityHeight - contentHeight);
                }
                case "middle" :
                {
                    // 居中对齐
                    return parseInt(correctNegative(contentCapacityHeight - contentHeight) / 2);
                }
                default:
                {
                    return 0;
                }
            }
        }

        function computeCapacityWidthOfBox(contentWidth) {
            var boxCapacityWidth = _config.width || contentWidth;

            // 如果非固定宽度,则根据当前宽度进行叠加
            if (!_config.width) {
                // 如果配置了padding,则需要加上2倍的padding(左右padding)
                if (_config.padding) {
                    boxCapacityWidth += _config.padding * 2;
                }

                // 加上左边框宽度
                if (arrayContain(_config.borders, 'left')) {
                    boxCapacityWidth++;
                }

                // 加上右边框宽度
                if (arrayContain(_config.borders, 'right')) {
                    boxCapacityWidth++;
                }
            }
            return boxCapacityWidth;
        }

        function computeCapacityHeightOfBox(contentHeight) {
            var boxCapacityHeight = _config.height || contentHeight;

            // 如果非固定高度,则根据当前高度进行叠加
            if (!_config.height) {
                // 加上上边框高度
                if (arrayContain(_config.borders, 'top')) {
                    boxCapacityHeight++;
                }

                // 加上下边框高度
                if (arrayContain(_config.borders, 'bottom')) {
                    boxCapacityHeight++;
                }
            }
            return boxCapacityHeight;
        }

        function renderingBorders(content, boxCapacityWidth, boxCapacityHeight) {

            var hasTopBorder = arrayContain(_config.borders, 'top');
            var hasBottomBorder = arrayContain(_config.borders, 'bottom');
            var hasLeftBorder = arrayContain(_config.borders, 'left');
            var hasRightBorder = arrayContain(_config.borders, 'right');
            var hasPadding = _config.padding ? true : false;

            var renderingContent = "";
            if (hasTopBorder) {
                var beginChar = hasLeftBorder ? '+' : '';
                var endChar = hasRightBorder ? '+' : '';
                var diff = beginChar.length + endChar.length;
                renderingContent += (beginChar + stringRepeat('-', boxCapacityWidth - diff) + endChar + '\n');
            }

            // 插入左边字符串 = 左边框+左padding
            var leftString = "";
            if (hasLeftBorder) {
                leftString += '|';
            }
            if (hasPadding) {
                leftString += stringBlank(_config.padding);
            }
            content = stringLeftInsert(content, leftString);

            // 插入右边字符串 = 右边框+右padding
            var rightString = "";
            if (hasPadding) {
                rightString += stringBlank(_config.padding);
            }
            if (hasRightBorder) {
                rightString += '|';
            }
            // 这里需要减去左padding的宽度
            content = textRightInsert(content, rightString, boxCapacityWidth - _config.padding);
            renderingContent += content;

            if (hasBottomBorder) {
                var beginChar = hasLeftBorder ? '+' : '';
                var endChar = hasRightBorder ? '+' : '';
                var diff = beginChar.length + endChar.length;
                renderingContent += ('\n' + beginChar + stringRepeat('-', boxCapacityWidth - diff) + endChar);
            }

            return renderingContent;
        }

        /**
         * 渲染容器(如果有必要)
         */
        function renderingIfNecessary() {
            if (!_isReRenderNecessary) {
                return;
            }

            // ---------- 宽度计算 ----------

            // 计算内容容量宽度
            var contentCapacityWidth = computeCapacityWidthOfContent();

            // 解析得到指定宽度换行好的文本内容
            var renderingContent = stringWrap(_content, contentCapacityWidth);

            // 计算内容宽度
            var contentWidth = textWidth(renderingContent);

            // 进行右偏移修正
            renderingContent = textRightShift(renderingContent, computeRightShift(contentCapacityWidth, contentWidth));

            // ---------- 高度计算 ----------

            // 计算内容高度
            var contentHeight = textHeight(renderingContent);

            // 计算内容容量高度
            var contentCapacityHeight = computeCapacityHeightOfContent(contentHeight);

            // 进行下偏移修正
            var bottomShift = computeBottomShift(contentCapacityHeight, contentHeight);
            renderingContent = textBottomShift(renderingContent, bottomShift);

            // 填充下偏移量
            renderingContent = renderingContent + stringRepeat('\n' + stringBlank(contentCapacityWidth), contentCapacityHeight - bottomShift - contentHeight);

            // ---------- 计算容器高度&宽度 ----------
            var boxCapacityWidth = computeCapacityWidthOfBox(contentWidth);
            var boxCapacityHeight = computeCapacityHeightOfBox(contentHeight);

            // ---------- 绘制边框 ----------
            var boxContent = renderingBorders(renderingContent, boxCapacityWidth, boxCapacityHeight);

            // ---------- 容器内容更新 ----------
            _width = textWidth(boxContent);
            _height = textHeight(boxContent);
            _boxContent = boxContent;
            _isReRenderNecessary = false;

        }

        /**
         * 容器宽度(单位:字符)
         */
        function width() {
            renderingIfNecessary();
            return _width;
        }

        /**
         * 容器高度(单位:字符)
         */
        function height() {
            renderingIfNecessary();
            return _height;
        }

        return {

            /**
             * 渲染容器
             */
            rendering: function () {
                renderingIfNecessary();
                return _boxContent;
            },

            /**
             * var config = {
             *     borders: ['top', 'bottom', 'left', 'right'],
             *     padding: 1,
             *     width: 100,
             *     height: 100
             * }
             */
            config: config,
            width: width,
            height: height,
        }
    }

})


//var config = {
//    borders: ['top', 'bottom', 'left', 'right','vertical', 'horizontal'],
//    padding: 1,
//    columns: [
//        {
//            width: 10,
//            vertical : 'middle'
//            horizontal: 'left'
//        },
//    ]
//}
//
//table.add([123, 456, 789])

/**
 * 定义一个表格
 * 表格基于容器而构建
 */
__greys_define('text-table-formatting', ['text-box-formatting'], function (box) {

    // 表格配置文件
    var _config = {};

    // 表格数据内容
    var _rows = [];

    // 是否需要重新渲染
    var _isReRenderNecessary = true;

    // 容器内容
    var _tableContent;

    // 解析配置
    function parse_config(config) {
        if (config) {
            _config.borders = config.borders || _config.borders;
            _config.padding = config.padding || _config.padding;
            _config.columns = config.columns || _config.columns;
        }
    }

    /**
     * 配置表格
     * @param config 配置
     */
    function config(config) {
        // config()
        if (arguments.length == 0) {
            _isReRenderNecessary = true;
            return _config;
        }

        // config(config)
        else if (arguments.length == 1) {
            parse_config(config);
            _isReRenderNecessary = true;
        }
    }

    /**
     * 添加一行
     */
    function row() {

        // 如果添加一个空行进来,则忽略
        if (arguments.length == 0) {
            return;
        }

        // 记录下一行的数据
        var cells = [];
        for (var index = 0; index < arguments.length; index++) {
            cells.push(arguments[index]);
        }
        _rows.push(cells);

    }

    /**
     * 计算行列矩阵(cell矩阵)
     * @returns {Array}
     */
    function computeMatrix() {
        // cell矩阵 cellMatrix
        var matrix = [];
        var hasVertical = arrayContain(_config.borders, 'vertical');
        var hasHorizontal = arrayContain(_config.borders, 'horizontal');
        var cellPadding = _config.padding;

        for (var rowIndex in _rows) {

            var isLastRow = rowIndex == _rows.length - 1;

            // 一行数据
            var row = _rows[rowIndex];

            // 一行数据封装
            var cells = [];

            // 计算一行最大高度
            var maxCellHeightInRow = 0;
            for (var colIndex in row) {

                // 如果一行的数据个数超过了列定义个数,则认为该数据不展示
                if (colIndex >= _config.columns.length) {
                    break;
                }

                var isLastCol = colIndex == _config.columns.length - 1;

                // 数据
                var data = row[colIndex];

                // 表格列配置
                var column = _config.columns[colIndex];

                var cell = new box(data);
                var cellBorders = [];
                if (hasVertical) {
                    cellBorders.push('right');
                }
                if (hasHorizontal) {
                    cellBorders.push('bottom');
                }
                // 最后一行需要去掉bottom
                if (isLastRow) {
                    arrayRemove(cellBorders, 'bottom');
                }
                // 最后一列需要去掉right
                if (isLastCol) {
                    arrayRemove(cellBorders, 'right');
                }

                cell.config({
                    padding: cellPadding,
                    borders: cellBorders,
                    vertical: column.vertical,
                    horizontal: column.horizontal,
                    width: column.width,
                    //height: maxCellHeightInRow
                });

                cell.rendering();
                maxCellHeightInRow = Math.max(maxCellHeightInRow, cell.height());
                cells.push(cell);

            }

            for (var index in cells) {
                var cell = cells[index];
                cell.config({
                    height: maxCellHeightInRow
                });
            }

            matrix.push(cells);
        }

        return matrix;
    }

    /**
     * 文本矩阵拼接
     * @param left  左文本
     * @param right 右文本
     * @returns {string}
     */
    function textCat(left, right) {
        var lWidth = textWidth(left);
        var rWidth = textWidth(right);
        var lefts = left.split(/\n/);
        var rights = right.split(/\n/);
        var height = Math.max(lefts.length, rights.length);
        var cat = "";
        for (var index = 0; index < height; index++) {
            var lContent = index < lefts.length ? stringRightPadding(lefts[index], lWidth) : stringBlank(lWidth);
            var rContent = index < rights.length ? stringRightPadding(rights[index], rWidth) : stringBlank(rWidth);
            cat += (lContent + rContent);
            if (index < height - 1) {
                cat += '\n';
            }
        }
        return cat;
    }

    /**
     * 渲染矩阵内容
     * @param matrix
     * @returns {string}
     */
    function renderingMatrix(matrix) {

        // 矩阵内容
        var matrixContent = "";

        for (var rowIndex in matrix) {

            var cells = matrix[rowIndex];
            var cellContent = "";

            // 渲染cell
            for (var colIndex in cells) {
                cellContent = textCat(cellContent, cells[colIndex].rendering());
            }

            if (rowIndex < matrix.length - 1) {
                cellContent += '\n';
            }

            matrixContent += cellContent;
        }

        return matrixContent;
    }

    /**
     * 渲染表格(如果有必要)
     */
    function renderingIfNecessary() {
        if (!_isReRenderNecessary) {
            return;
        }

        // 计算&渲染表格矩阵内容
        var matrixContent = renderingMatrix(computeMatrix());

        // 渲染表格外边框
        var tableBox = new box(matrixContent);
        tableBox.config({
            borders: _config.borders,
        });

        _tableContent = tableBox.rendering();
        _isReRenderNecessary = false;

    }

    return function () {
        return {

            /**
             * 配置表格
             */
            config: config,

            row: row,

            /**
             * 渲染容器
             */
            rendering: function () {
                renderingIfNecessary();
                return _tableContent;
            },
        }
    }
})

__greys_define('text-formatting', ['text-box-formatting', 'text-table-formatting'], function (box, table) {
    return {
        box: box,
        table: table,
    }
})

__greys_require(['text-formatting'], function (text) {
    module.id = 'text-formatting';
    module.exports = text;
})