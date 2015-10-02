package com.github.ompc.greys.core.view;

import com.github.ompc.greys.core.manager.TimeFragmentManager.TimeFragment;
import com.github.ompc.greys.core.util.Advice;

import java.text.SimpleDateFormat;

import static java.lang.Integer.toHexString;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;

/**
 * 时间片段表格
 * Created by vlinux on 15/10/3.
 */
public class TimeFragmentTableView implements View {

    /*
     * 各列宽度
     */
    private static final int[] TABLE_COL_WIDTH = new int[]{
            8, // index
            8, // processId
            20, // timestamp
            10, // cost(ms)
            8, // isRet
            8, // isExp
            15, // object address
            30, // class
            30, // method
    };

    /*
     * 各列名称
     */
    private static final String[] TABLE_COL_TITLE = new String[]{
            "INDEX",
            "PROCESS",
            "TIMESTAMP",
            "COST(ms)",
            "IS-RET",
            "IS-EXP",
            "OBJECT",
            "CLASS",
            "METHOD"

    };

    private final TableView tableView;

    public TimeFragmentTableView(boolean isPrintTitle) {
        this.tableView = new TableView(
                new TableView.ColumnDefine[]{
                        new TableView.ColumnDefine(TABLE_COL_WIDTH[0], false, TableView.Align.RIGHT),
                        new TableView.ColumnDefine(TABLE_COL_WIDTH[1], false, TableView.Align.RIGHT),
                        new TableView.ColumnDefine(TABLE_COL_WIDTH[2], false, TableView.Align.RIGHT),
                        new TableView.ColumnDefine(TABLE_COL_WIDTH[3], false, TableView.Align.RIGHT),
                        new TableView.ColumnDefine(TABLE_COL_WIDTH[4], false, TableView.Align.RIGHT),
                        new TableView.ColumnDefine(TABLE_COL_WIDTH[5], false, TableView.Align.RIGHT),
                        new TableView.ColumnDefine(TABLE_COL_WIDTH[6], false, TableView.Align.RIGHT),
                        new TableView.ColumnDefine(TABLE_COL_WIDTH[7], false, TableView.Align.RIGHT),
                        new TableView.ColumnDefine(TABLE_COL_WIDTH[8], false, TableView.Align.RIGHT)
                }
        ).hasBorder(true).padding(1);

        if (isPrintTitle) {
            fillTableTitle();
        }

    }

    /**
     * 添加标题
     */
    private void fillTableTitle() {
        this.tableView.addRow(
                TABLE_COL_TITLE[0],
                TABLE_COL_TITLE[1],
                TABLE_COL_TITLE[2],
                TABLE_COL_TITLE[3],
                TABLE_COL_TITLE[4],
                TABLE_COL_TITLE[5],
                TABLE_COL_TITLE[6],
                TABLE_COL_TITLE[7],
                TABLE_COL_TITLE[8]
        );
    }

    /*
     * 填充表格行
     */
    public TimeFragmentTableView add(TimeFragment timeFragment) {
        final Advice advice = timeFragment.advice;
        tableView.addRow(
                timeFragment.id,
                timeFragment.processId,
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(timeFragment.gmtCreate),
                timeFragment.cost,
                advice.isAfterReturning(),
                advice.isAfterThrowing(),
                advice.getTarget() == null
                        ? "NULL"
                        : "0x" + toHexString(advice.getTarget().hashCode()),
                substringAfterLast("." + advice.getClazz().getName(), "."),
                advice.getMethod().getName()
        );
        return this;
    }

    /**
     * 关闭下边框
     */
    public TimeFragmentTableView turnOffBottom() {
        tableView.borders(tableView.borders() & ~TableView.BORDER_BOTTOM);
        return this;
    }

    /**
     * 打开下边框
     */
    public TimeFragmentTableView turnOnBottom() {
        tableView.borders(tableView.borders() | TableView.BORDER_BOTTOM);
        return this;
    }

    @Override
    public String draw() {
        return tableView.draw();
    }
}
