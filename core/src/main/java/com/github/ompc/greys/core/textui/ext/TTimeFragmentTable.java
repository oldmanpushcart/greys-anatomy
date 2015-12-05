package com.github.ompc.greys.core.textui.ext;

import com.github.ompc.greys.core.Advice;
import com.github.ompc.greys.core.TimeFragment;
import com.github.ompc.greys.core.textui.TComponent;
import com.github.ompc.greys.core.textui.TTable;
import com.github.ompc.greys.core.util.SimpleDateFormatHolder;

import static com.github.ompc.greys.core.util.GaStringUtils.hashCodeToHexString;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;

/**
 * 时间片段表格
 * Created by vlinux on 15/10/3.
 */
public class TTimeFragmentTable implements TComponent {

    /*
     * 各列宽度
     */
    private static final int[] TABLE_COL_WIDTH = new int[]{
            8, // index
            10, // processId
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
            "PROCESS-ID",
            "TIMESTAMP",
            "COST(ms)",
            "IS-RET",
            "IS-EXP",
            "OBJECT",
            "CLASS",
            "METHOD"

    };

    private final TTable tTable;

    public TTimeFragmentTable(boolean isPrintTitle) {
        this.tTable = new TTable(
                new TTable.ColumnDefine[]{
                        new TTable.ColumnDefine(TABLE_COL_WIDTH[0], false, TTable.Align.RIGHT),
                        new TTable.ColumnDefine(TABLE_COL_WIDTH[1], false, TTable.Align.RIGHT),
                        new TTable.ColumnDefine(TABLE_COL_WIDTH[2], false, TTable.Align.RIGHT),
                        new TTable.ColumnDefine(TABLE_COL_WIDTH[3], false, TTable.Align.RIGHT),
                        new TTable.ColumnDefine(TABLE_COL_WIDTH[4], false, TTable.Align.RIGHT),
                        new TTable.ColumnDefine(TABLE_COL_WIDTH[5], false, TTable.Align.RIGHT),
                        new TTable.ColumnDefine(TABLE_COL_WIDTH[6], false, TTable.Align.RIGHT),
                        new TTable.ColumnDefine(TABLE_COL_WIDTH[7], false, TTable.Align.RIGHT),
                        new TTable.ColumnDefine(TABLE_COL_WIDTH[8], false, TTable.Align.RIGHT)
                }
        ).padding(1);

        if (isPrintTitle) {
            fillTableTitle();
        }

    }

    /**
     * 添加标题
     */
    private void fillTableTitle() {
        this.tTable.addRow(
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
    public TTimeFragmentTable add(TimeFragment timeFragment) {
        final Advice advice = timeFragment.advice;
        tTable.addRow(
                timeFragment.id,
                timeFragment.processId,
                SimpleDateFormatHolder.getInstance().format(timeFragment.gmtCreate),
                timeFragment.cost,
                advice.isReturn,
                advice.isThrow,
                hashCodeToHexString(advice.target),
                substringAfterLast("." + advice.clazz.getName(), "."),
                advice.method.getName()
        );
        return this;
    }

    /**
     * 关闭下边框
     */
    public TTimeFragmentTable turnOffBottom() {
        tTable.getBorder().remove(TTable.Border.BORDER_OUTER_BOTTOM);
        return this;
    }

    /**
     * 打开下边框
     */
    public TTimeFragmentTable turnOnBottom() {
        tTable.getBorder().add(TTable.Border.BORDER_OUTER_BOTTOM);
        return this;
    }

    @Override
    public String rendering() {
        return tTable.rendering();
    }
}
