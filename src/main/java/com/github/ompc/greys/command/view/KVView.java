package com.github.ompc.greys.command.view;

import com.github.ompc.greys.command.view.TableView.ColumnDefine;

import static com.github.ompc.greys.command.view.TableView.Align.LEFT;
import static com.github.ompc.greys.command.view.TableView.Align.RIGHT;

/**
 * KV排版控件
 * Created by vlinux on 15/5/9.
 */
public class KVView implements View {

    private final TableView tableView;

    public KVView() {
        this.tableView = new TableView(new ColumnDefine[]{
                new ColumnDefine(RIGHT),
                new ColumnDefine(RIGHT),
                new ColumnDefine(LEFT)
        })
                .hasBorder(false)
                .padding(0);
    }

    public KVView(ColumnDefine keyColumnDefine, ColumnDefine valueColumnDefine) {
        this.tableView = new TableView(new ColumnDefine[]{
                keyColumnDefine,
                new ColumnDefine(RIGHT),
                valueColumnDefine
        })
                .hasBorder(false)
                .padding(0);
    }

    public KVView add(final Object key, final Object value) {
        tableView.addRow(key, " : ", value);
        return this;
    }

    @Override
    public String draw() {
        return tableView.draw();
    }
}
