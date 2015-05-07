package com.googlecode.greysanatomy.command.view;

import java.util.ArrayList;
import java.util.List;

import static com.googlecode.greysanatomy.util.GaStringUtils.*;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.String.format;

/**
 * 表格控件
 * Created by vlinux on 15/5/7.
 */
public class TableView implements View {

    private final ColumnDefine[] columnDefineArray;

    // 是否渲染边框
    private boolean drawBorder;

    // 内填充
    private int padding;

    public TableView(ColumnDefine[] columnDefineArray) {
        this.columnDefineArray = null == columnDefineArray
                ? new ColumnDefine[0]
                : columnDefineArray;
    }

    public TableView(int columnNum) {
        this.columnDefineArray = new ColumnDefine[columnNum];
        for (int index = 0; index < columnDefineArray.length; index++) {
            columnDefineArray[index] = new ColumnDefine();
        }
    }

    @Override
    public String draw() {
        final StringBuilder tableSB = new StringBuilder();

        // init width cache
        final int[] widthCacheArray = new int[columnDefineArray.length];
        for (int index = 0; index < columnDefineArray.length; index++) {
            widthCacheArray[index] = abs(columnDefineArray[index].getWidth());
        }

        final int tableHigh = getTableHigh();
        for (int rowIndex = 0; rowIndex < tableHigh; rowIndex++) {

            final boolean isLastRow = rowIndex == tableHigh - 1;

            // 打印分割行
            if (isDrawBorder()) {
                tableSB.append(drawSeparationLine(widthCacheArray)).append("\n");
            }


            // 打印正式数据
            for (int colIndex = 0; colIndex < widthCacheArray.length; colIndex++) {

                final boolean isLastColOfRow = colIndex == widthCacheArray.length - 1;
                final ColumnDefine columnDefine = columnDefineArray[colIndex];

                // 空指针保护
                if (null == columnDefine) {
                    continue;
                }

                final String data;
                // 当前列已经读尽，此列为空数据
                if (columnDefine.getHigh() <= rowIndex) {
                    data = EMPTY;
                } else {
                    data = columnDefine.dataList.get(rowIndex);
                }

                final int width = widthCacheArray[colIndex];
                final String dataFormat;
                switch (columnDefine.align) {
                    case RIGHT: {
                        dataFormat = "%" + width + "s";
                        break;
                    }
                    case LEFT:
                    default: {
                        dataFormat = "%-" + width + "s";
                        break;
                    }
                }


                final String borderChar = drawBorder
                        ? "|"
                        : EMPTY;

                if (width > 0) {
                    tableSB.append(format(borderChar + repeat(" ", padding) + dataFormat + repeat(" ", padding), summary(data, width)));
                }

                if (isLastColOfRow) {
                    tableSB.append(borderChar).append("\n");
                }
            }


            // 打印结尾分隔行
            if (isLastRow
                    && isDrawBorder()) {
                // 打印分割行
                tableSB.append(drawSeparationLine(widthCacheArray));
            }

        }


        return tableSB.toString();
    }

    /*
     * 获取表格高度
     */
    private int getTableHigh() {
        int tableHigh = 0;
        for (ColumnDefine columnDefine : columnDefineArray) {
            tableHigh = max(tableHigh, columnDefine.getHigh());
        }
        return tableHigh;
    }

    /*
     * 打印分隔行
     */
    private String drawSeparationLine(int[] widthCacheArray) {
        final StringBuilder separationLineSB = new StringBuilder();

        for (int width : widthCacheArray) {
            if (width > 0) {
                separationLineSB.append("+").append(repeat("-", width + 2 * padding));
            }
        }

        separationLineSB.append("+");

        return separationLineSB.toString();
    }


    /**
     * 添加数据行
     *
     * @param columnDataArray 数据数组
     */
    public TableView addRow(Object... columnDataArray) {
        if (null == columnDataArray) {
            return this;
        }

        for (int index = 0; index < columnDefineArray.length; index++) {
            final ColumnDefine columnDefine = columnDefineArray[index];
            if (index < columnDataArray.length
                    && null != columnDataArray[index]) {
                columnDefine.dataList.add(columnDataArray[index].toString());
            } else {
                columnDefine.dataList.add(EMPTY);
            }
        }

        return this;

    }


    /**
     * 对齐方向
     */
    public enum Align {
        LEFT,
        RIGHT
    }

    /**
     * 列定义
     */
    public static class ColumnDefine {

        private final int width;
        private final boolean isAutoResize;
        private final Align align;
        private final List<String> dataList = new ArrayList<String>();

        public ColumnDefine(int width, boolean isAutoResize, Align align) {
            this.width = width;
            this.isAutoResize = isAutoResize;
            this.align = align;
        }

        public ColumnDefine(Align align) {
            this(0, true, align);
        }

        public ColumnDefine() {
            this(Align.LEFT);
        }

        /**
         * 获取当前列的宽度
         *
         * @return 宽度
         */
        public int getWidth() {

            if (!isAutoResize) {
                return width;
            }

            int maxWidth = 0;
            for (String data : dataList) {
                maxWidth = max(length(data), maxWidth);
            }

            return maxWidth;
        }

        /**
         * 获取当前列的高度
         *
         * @return 高度
         */
        public int getHigh() {
            return dataList.size();
        }

    }

    /**
     * 设置是否画边框
     *
     * @param isDrawBorder true / false
     */
    public void setDrawBorder(boolean isDrawBorder) {
        this.drawBorder = isDrawBorder;
    }

    /**
     * 是否画边框
     *
     * @return true / false
     */
    public boolean isDrawBorder() {
        return drawBorder;
    }

    /**
     * 设置内边距大小
     *
     * @param padding 内边距
     */
    public void setPadding(int padding) {
        this.padding = padding;
    }

    public static void main(String... args) {


        final TableView tv = new TableView(new ColumnDefine[]{
                new ColumnDefine(10, false, Align.RIGHT),
                new ColumnDefine(1, false, Align.LEFT),
                new ColumnDefine(0, true, Align.LEFT),
        });

        tv.setDrawBorder(false);
        tv.setPadding(0);

        tv.addRow(
                "1AAAAaaaaaaaaaaaaaaaaaaaaaaa",
                ":",
                "CCCCC"
        );

        tv.addRow(
                "2AAAAA",
                ":",
                "CCCCC"

        );

        tv.addRow(
                "3AAAAA",
                ":",
                "CCCCC",
                "3DDDDD"
        );


        System.out.println(tv.draw());

    }

}
