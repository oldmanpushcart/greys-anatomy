package com.github.ompc.greys.core.command.view;

/**
 * KV排版控件
 * Created by vlinux on 15/5/9.
 */
public class KVView implements View {

    private final TableView tableView;

    public KVView() {
        this.tableView = new TableView(new TableView.ColumnDefine[]{
                new TableView.ColumnDefine(TableView.Align.RIGHT),
                new TableView.ColumnDefine(TableView.Align.RIGHT),
                new TableView.ColumnDefine(TableView.Align.LEFT)
        })
                .hasBorder(false)
                .padding(0);
    }

    public KVView(TableView.ColumnDefine keyColumnDefine, TableView.ColumnDefine valueColumnDefine) {
        this.tableView = new TableView(new TableView.ColumnDefine[]{
                keyColumnDefine,
                new TableView.ColumnDefine(TableView.Align.RIGHT),
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
