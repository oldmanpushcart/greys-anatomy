package com.github.ompc.greys.core.textui.ext;

import com.github.ompc.greys.core.Advice;
import com.github.ompc.greys.core.GlobalOptions;
import com.github.ompc.greys.core.TimeFragment;
import com.github.ompc.greys.core.textui.TComponent;
import com.github.ompc.greys.core.textui.TTable;
import com.github.ompc.greys.core.textui.TTable.ColumnDefine;
import com.github.ompc.greys.core.util.GaStringUtils;
import com.github.ompc.greys.core.util.SimpleDateFormatHolder;
import com.github.ompc.greys.core.util.SizeOf;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.Instrumentation;

import static com.github.ompc.greys.core.textui.TTable.Align.LEFT;
import static com.github.ompc.greys.core.textui.TTable.Align.RIGHT;

/**
 * 时间碎片详情展示
 * Created by oldmanpushcart@gmail.com on 15/10/3.
 */
public class TTimeFragmentDetail implements TComponent {

    private final Instrumentation inst;
    private final TimeFragment timeFragment;
    private final Integer expend;

    public TTimeFragmentDetail(final Instrumentation inst, final TimeFragment timeFragment, final Integer expend) {
        this.inst = inst;
        this.timeFragment = timeFragment;
        this.expend = expend;
    }

    /**
     * 是否需要展开输出对象
     */
    private boolean isNeedExpend() {
        return null != expend
                && expend > 0;
    }

    @Override
    public String rendering() {

        final Advice advice = timeFragment.advice;
        final String className = advice.clazz.getName();
        final String methodName = advice.method.getName();

        final TTable tTable = new TTable(
                new ColumnDefine[]{
                        new ColumnDefine(15, false, RIGHT),
                        new ColumnDefine(150, false, LEFT)
                })
                .padding(1)
                .addRow("INDEX", timeFragment.id)
                .addRow("PROCESS-ID", timeFragment.processId)
                .addRow("GMT-CREATE", SimpleDateFormatHolder.getInstance().format(timeFragment.gmtCreate))
                .addRow("COST(ms)", timeFragment.cost)
                .addRow("OBJECT", GaStringUtils.hashCodeToHexString(advice.target))
                .addRow("CLASS", className)
                .addRow("METHOD", methodName)
                .addRow("IS-RETURN", advice.isReturn)
                .addRow("IS-EXCEPTION", advice.isThrow);

        // fill the parameters
        if (null != advice.params) {

            int paramIndex = 0;
            for (Object param : advice.params) {

                final StringBuilder titleSB = new StringBuilder("PARAMETERS[" + paramIndex++ + "]");
                if (GlobalOptions.isDisplayObjectSize) {
                    titleSB.append("\n").append(new SizeOf(inst, param, SizeOf.HeapType.SHALLOW));
                    titleSB.append("\n").append(new SizeOf(inst, param, SizeOf.HeapType.RETAINED));
                }
                tTable.addRow(titleSB.toString(), new TObject(param, expend).rendering());
            }

        }

        // fill the returnObj
        if (!advice.isThrow) {

            final StringBuilder titleSB = new StringBuilder("RETURN-OBJ");
            if (GlobalOptions.isDisplayObjectSize) {
                titleSB.append("\n").append(new SizeOf(inst, advice.returnObj, SizeOf.HeapType.SHALLOW));
                titleSB.append("\n").append(new SizeOf(inst, advice.returnObj, SizeOf.HeapType.RETAINED));
            }
            tTable.addRow(
                    titleSB.toString(),
                    new TObject(advice.returnObj, expend).rendering()
            );

        }

        // fill the throw exception
        if (advice.isThrow) {

            //noinspection ThrowableResultOfMethodCallIgnored
            final Throwable throwable = advice.throwExp;

            final StringBuilder titleSB = new StringBuilder("THROW-EXCEPTION");
            if (GlobalOptions.isDisplayObjectSize) {
                titleSB.append("\n").append(new SizeOf(inst, advice.throwExp, SizeOf.HeapType.SHALLOW));
                titleSB.append("\n").append(new SizeOf(inst, advice.throwExp, SizeOf.HeapType.RETAINED));
            }

            if (isNeedExpend()) {
                tTable.addRow(titleSB.toString(), new TObject(advice.throwExp, expend).rendering());
            } else {
                final StringWriter stringWriter = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(stringWriter);
                try {
                    throwable.printStackTrace(printWriter);
                    tTable.addRow(titleSB.toString(), stringWriter.toString());
                } finally {
                    printWriter.close();
                }

            }

        }

        // fill the stack
        tTable.addRow("STACK", timeFragment.stack);

        return tTable.rendering();
    }
}
