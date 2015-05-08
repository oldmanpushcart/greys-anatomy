package com.googlecode.greysanatomy.command.view;

import com.googlecode.greysanatomy.command.view.TableView.ColumnDefine;

import static com.googlecode.greysanatomy.command.view.TableView.Align.LEFT;
import static com.googlecode.greysanatomy.command.view.TableView.Align.RIGHT;

/**
 * KVÅÅ°æ¿Ø¼þ
 * Created by vlinux on 15/5/9.
 */
public class KeyValueView implements View {

    private final TableView tableView;

    public KeyValueView() {
        this.tableView = new TableView(new ColumnDefine[]{
                new ColumnDefine(RIGHT),
                new ColumnDefine(RIGHT),
                new ColumnDefine(LEFT)
        })
                .setDrawBorder(false)
                .setPadding(0);
    }

    public KeyValueView add(final Object key, final Object value) {
        tableView.addRow(key, " : ", value);
        return this;
    }

    @Override
    public String draw() {
        return tableView.draw();
    }
}
