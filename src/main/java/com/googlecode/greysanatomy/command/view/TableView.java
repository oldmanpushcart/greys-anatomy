package com.googlecode.greysanatomy.command.view;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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
        for (int index = 0; index < this.columnDefineArray.length; index++) {
            columnDefineArray[index] = new ColumnDefine();
        }
    }

    @Override
    public String draw() {
        final StringBuilder tableSB = new StringBuilder();

        // init width cache
        final int[] widthCacheArray = new int[getColumnCount()];
        for (int index = 0; index < widthCacheArray.length; index++) {
            widthCacheArray[index] = abs(columnDefineArray[index].getWidth());
        }

        final int tableHigh = getTableHigh();
        for (int rowIndex = 0; rowIndex < tableHigh; rowIndex++) {

            final boolean isLastRow = rowIndex == tableHigh - 1;

            // 打印分割行
            if (isDrawBorder()) {
                tableSB.append(drawSeparationLine(widthCacheArray)).append("\n");
            }

            // 绘一行
            drawLine(tableSB, widthCacheArray, rowIndex);


            // 打印结尾分隔行
            if (isLastRow
                    && isDrawBorder()) {
                // 打印分割行
                tableSB.append(drawSeparationLine(widthCacheArray));
            }

        }


        return tableSB.toString();
    }


    private void drawLine(StringBuilder tableSB, int[] widthCacheArray, int rowIndex) {

        final Scanner[] scannerArray = new Scanner[getColumnCount()];
        try {
            boolean hasNext;
            do {

                hasNext = false;
                final StringBuilder segmentSB = new StringBuilder();

                for (int colIndex = 0; colIndex < getColumnCount(); colIndex++) {

                    if (null == scannerArray[colIndex]) {
                        scannerArray[colIndex] = new Scanner(
                                new StringReader(
                                        getData(rowIndex, columnDefineArray[colIndex])));
                    }

                    final String borderChar = isDrawBorder() ? "|" : EMPTY;
                    final int width = widthCacheArray[colIndex];
                    final boolean isLastColOfRow = colIndex == widthCacheArray.length - 1;
                    final Scanner scanner = scannerArray[colIndex];

                    final String data;
                    if (scanner.hasNext()) {
                        data = scanner.nextLine();
                        hasNext = true;
                    } else {
                        data = EMPTY;
                    }

                    if (width > 0) {

                        final ColumnDefine columnDefine = columnDefineArray[colIndex];
                        final String dataFormat = getDataFormat(columnDefine, width);
                        final String paddingChar = repeat(" ", padding);

                        segmentSB.append(
                                format(borderChar + paddingChar + dataFormat + paddingChar,
                                        summary(data, width)));

                    }

                    if (isLastColOfRow) {
                        segmentSB.append(borderChar).append("\n");
                    }

                }

                if (hasNext) {
                    tableSB.append(segmentSB);
                }

            } while (hasNext);
        } finally {
            for (Scanner scanner : scannerArray) {
                if( null != scanner ) {
                    scanner.close();
                }
            }
        }

    }

    private String getData(int rowIndex, ColumnDefine columnDefine) {
        return columnDefine.getHigh() <= rowIndex
                ? EMPTY
                : columnDefine.dataList.get(rowIndex);
    }

    private String getDataFormat(ColumnDefine columnDefine, int width) {
        switch (columnDefine.align) {
            case RIGHT: {
                return "%" + width + "s";
            }
            case LEFT:
            default: {
                return "%-" + width + "s";
            }
        }
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
                final Scanner scanner = new Scanner(new StringReader(data));
                try {
                    while (scanner.hasNext()) {
                        maxWidth = max(length(scanner.nextLine()), maxWidth);
                    }
                } finally {
                    scanner.close();
                }
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
    public TableView setDrawBorder(boolean isDrawBorder) {
        this.drawBorder = isDrawBorder;
        return this;
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
    public TableView setPadding(int padding) {
        this.padding = padding;
        return this;
    }

    /**
     * 获取表格列总数
     *
     * @return 表格列总数
     */
    public int getColumnCount() {
        return columnDefineArray.length;
    }

    public static void main(String... args) {


        final TableView tv = new TableView(new ColumnDefine[]{
                new ColumnDefine(10, false, Align.RIGHT),
                new ColumnDefine(0, true, Align.LEFT),
        });

        tv.setDrawBorder(false);
        tv.setPadding(0);

        tv.addRow(
                "AAAAaaaaaaaaaaaaaaaaaaaaaaa",
                "CCCCC"
        );

        tv.addRow(
                "AAAAA",
                "CCC1C\n\n\n3DDDD"

        );

        tv.addRow(
                "AAAAA",
                "CCCCC",
                "DDDDD"
        );


        System.out.println(tv.draw());

    }

}
