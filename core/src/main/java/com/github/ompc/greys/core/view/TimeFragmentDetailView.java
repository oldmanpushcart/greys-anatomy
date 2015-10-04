package com.github.ompc.greys.core.view;

import com.github.ompc.greys.core.TimeFragment;
import com.github.ompc.greys.core.Advice;
import com.github.ompc.greys.core.util.GaStringUtils;
import com.github.ompc.greys.core.view.TableView.ColumnDefine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;

import static com.github.ompc.greys.core.view.TableView.Align.LEFT;
import static com.github.ompc.greys.core.view.TableView.Align.RIGHT;

/**
 * 时间碎片详情展示
 * Created by vlinux on 15/10/3.
 */
public class TimeFragmentDetailView implements View {

    private final TimeFragment timeFragment;
    private final Integer expend;

    public TimeFragmentDetailView(final TimeFragment timeFragment, final Integer expend) {
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
    public String draw() {

        final Advice advice = timeFragment.advice;
        final String className = advice.clazz.getName();
        final String methodName = advice.method.getName();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        final TableView view = new TableView(
                new ColumnDefine[]{
                        new ColumnDefine(15, false, RIGHT),
                        new ColumnDefine(150, false, LEFT)
                })
                .hasBorder(true)
                .padding(1)
                .addRow("INDEX", timeFragment.id)
                .addRow("PROCESS-ID", timeFragment.processId)
                .addRow("GMT-CREATE", sdf.format(timeFragment.gmtCreate))
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

                if (isNeedExpend()) {
                    view.addRow("PARAMETERS[" + paramIndex++ + "]", new ObjectView(param, expend).draw());
                } else {
                    view.addRow("PARAMETERS[" + paramIndex++ + "]", param);
                }

            }

        }

        // fill the returnObj
        if (advice.isThrow) {

            view.addRow(
                    "RETURN-OBJ",
                    isNeedExpend()
                            ? new ObjectView(advice.returnObj, expend).draw()
                            : advice.returnObj
            );

        }

        // fill the throw exception
        if (advice.isThrow) {

            //noinspection ThrowableResultOfMethodCallIgnored
            final Throwable throwable = advice.throwExp;

            if (isNeedExpend()) {
                view.addRow("THROW-EXCEPTION", new ObjectView(advice.throwExp, expend).draw());
            } else {
                final StringWriter stringWriter = new StringWriter();
                final PrintWriter printWriter = new PrintWriter(stringWriter);
                try {
                    throwable.printStackTrace(printWriter);
                    view.addRow("THROW-EXCEPTION", stringWriter.toString());
                } finally {
                    printWriter.close();
                }

            }

        }

        // fill the stack
        view.addRow("STACK", timeFragment.stack);

        return view.draw();
    }
}
