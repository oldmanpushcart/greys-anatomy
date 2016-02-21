__greys_require({
    paths: {
        'common-lang': 'https://raw.githubusercontent.com/oldmanpushcart/greys-anatomy/master/scripts/common-lang-module.js',
    }
});

/**
 * 字符串空白填充
 * @param lang   lang
 * @param length 填充长度
 * @returns {string}
 */
function stringBlank(lang, length) {
    return lang.string.repeat(' ', length);
}

/**
 * box-formatting
 * 定义一个容器(box),容器是所有文本组件的基础
 */
__greys_define('text-box-formatting', ['common-lang'], function (lang) {

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
            var contentCapacityWidth = _config.width || lang.text.width(_content);

            // 固定宽度需要对padding和边框宽度进行修正
            if (_config.width) {
                // 如果配置了padding,则需要减去2倍的padding(左右padding)
                if (_config.padding) {
                    contentCapacityWidth -= _config.padding * 2;
                }

                // 剪去左边框宽度
                if (lang.array.contains(_config.borders, 'left')) {
                    contentCapacityWidth--;
                }

                // 减去右边框宽度
                if (lang.array.contains(_config.borders, 'right')) {
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
                if (lang.array.contains(_config.borders, 'top')) {
                    contentCapacityHeight--;
                }

                // 减去下边框高度
                if (lang.array.contains(_config.borders, 'bottom')) {
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
                if (lang.array.contains(_config.borders, 'left')) {
                    boxCapacityWidth++;
                }

                // 加上右边框宽度
                if (lang.array.contains(_config.borders, 'right')) {
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
                if (lang.array.contains(_config.borders, 'top')) {
                    boxCapacityHeight++;
                }

                // 加上下边框高度
                if (lang.array.contains(_config.borders, 'bottom')) {
                    boxCapacityHeight++;
                }
            }
            return boxCapacityHeight;
        }

        function renderingBorders(content, boxCapacityWidth, boxCapacityHeight) {

            var hasTopBorder = lang.array.contains(_config.borders, 'top');
            var hasBottomBorder = lang.array.contains(_config.borders, 'bottom');
            var hasLeftBorder = lang.array.contains(_config.borders, 'left');
            var hasRightBorder = lang.array.contains(_config.borders, 'right');
            var hasPadding = _config.padding ? true : false;

            var renderingContent = "";
            if (hasTopBorder) {
                var beginChar = hasLeftBorder ? '+' : '';
                var endChar = hasRightBorder ? '+' : '';
                var diff = beginChar.length + endChar.length;
                renderingContent += (beginChar + lang.string.repeat('-', boxCapacityWidth - diff) + endChar + '\n');
            }

            // 插入左边字符串 = 左边框+左padding
            var leftString = "";
            if (hasLeftBorder) {
                leftString += '|';
            }
            if (hasPadding) {
                leftString += stringBlank(lang, _config.padding);
            }
            content = lang.text.insertLeft(content, leftString);

            // 插入右边字符串 = 右边框+右padding
            var rightString = "";
            if (hasPadding) {
                rightString += stringBlank(lang, _config.padding);
            }
            if (hasRightBorder) {
                rightString += '|';
            }
            // 这里需要减去左padding的宽度
            content = lang.text.insertRight(content, rightString, boxCapacityWidth - _config.padding);
            renderingContent += content;

            if (hasBottomBorder) {
                var beginChar = hasLeftBorder ? '+' : '';
                var endChar = hasRightBorder ? '+' : '';
                var diff = beginChar.length + endChar.length;
                renderingContent += ('\n' + beginChar + lang.string.repeat('-', boxCapacityWidth - diff) + endChar);
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
            var renderingContent = lang.string.wrap(_content, contentCapacityWidth);

            // 计算内容宽度
            var contentWidth = lang.text.width(renderingContent);

            // 进行右偏移修正
            renderingContent = lang.text.shiftRight(renderingContent, computeRightShift(contentCapacityWidth, contentWidth));

            // ---------- 高度计算 ----------

            // 计算内容高度
            var contentHeight = lang.text.height(renderingContent);

            // 计算内容容量高度
            var contentCapacityHeight = computeCapacityHeightOfContent(contentHeight);

            // 进行下偏移修正
            var bottomShift = computeBottomShift(contentCapacityHeight, contentHeight);
            renderingContent = lang.text.shiftBottom(renderingContent, bottomShift);

            // 填充下偏移量
            renderingContent = renderingContent + lang.string.repeat('\n' + stringBlank(lang, contentCapacityWidth), contentCapacityHeight - bottomShift - contentHeight);

            // ---------- 计算容器高度&宽度 ----------
            var boxCapacityWidth = computeCapacityWidthOfBox(contentWidth);
            var boxCapacityHeight = computeCapacityHeightOfBox(contentHeight);

            // ---------- 绘制边框 ----------
            var boxContent = renderingBorders(renderingContent, boxCapacityWidth, boxCapacityHeight);

            // ---------- 容器内容更新 ----------
            //_width = lang.text.width(boxContent);
            _width = boxCapacityWidth;
            //_height = lang.text.height(boxContent);
            _height = boxCapacityHeight;
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
__greys_define('text-table-formatting', ['text-box-formatting', 'common-lang'], function (box, lang) {

    return function () {
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
            var hasVertical = lang.array.contains(_config.borders, 'vertical');
            var hasHorizontal = lang.array.contains(_config.borders, 'horizontal');
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
                        lang.array.remove(cellBorders, 'bottom');
                    }
                    // 最后一列需要去掉right
                    if (isLastCol) {
                        lang.array.remove(cellBorders, 'right');
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

                lang.array.forEach(cells, function (index, cell) {
                    cell.config({
                        height: maxCellHeightInRow
                    });
                })

                matrix.push(cells);
            }

            return matrix;
        }

        /**
         * 字符串空白右填充
         * @param length 填充长度
         * @returns {string}
         */
        function stringRightPadding(string, length) {
            var empty = stringBlank(lang, Math.max(string.length, length));
            var padding = "";
            lang.string.forEach(empty, function (index, c) {
                padding += index < string.length ? string[index] : c;
            });
            return padding;
        }

        /**
         * 文本矩阵拼接
         * @param left  左文本
         * @param right 右文本
         * @returns {string}
         */
        function textCat(left, right) {
            var lWidth = lang.text.width(left);
            var rWidth = lang.text.width(right);
            var lefts = left.split(/\n/);
            var rights = right.split(/\n/);
            var height = Math.max(lefts.length, rights.length);
            var cat = "";
            for (var index = 0; index < height; index++) {
                var lContent = index < lefts.length ? stringRightPadding(lefts[index], lWidth) : stringBlank(lang, lWidth);
                var rContent = index < rights.length ? stringRightPadding(rights[index], rWidth) : stringBlank(lang, rWidth);
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

            lang.array.forEach(matrix, function (rowIndex, cells) {

                var cellContent = "";

                // 渲染cell
                lang.array.forEach(cells, function (colIndex, cell) {
                    cellContent = textCat(cellContent, cell.rendering());
                })

                if (rowIndex < matrix.length - 1) {
                    cellContent += '\n';
                }

                matrixContent += cellContent;
            })

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
    module.exports = text;
})