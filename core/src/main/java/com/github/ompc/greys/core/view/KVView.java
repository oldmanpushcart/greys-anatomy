package com.github.ompc.greys.core.view;

import org.apache.commons.lang3.StringUtils;

import java.util.Scanner;

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
        return filterEmptyLine(tableView.draw());
    }


    /*
     * 出现多余的空行的原因是，KVview在输出时，会补全空格到最长的长度。所以在"yyyyy”后面会多出来很多的空格。
     * 再经过TableView的固定列处理，多余的空格就会在一行里放不下，输出成两行（第二行前面是空格）
     *
     * @see https://github.com/oldmanpushcart/greys-anatomy/issues/82
     */
    private String filterEmptyLine(String content) {
        final StringBuilder sb = new StringBuilder();
        Scanner scanner = null;
        try {
            scanner = new Scanner(content);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line != null) {
                    //清理一行后面多余的空格
                    line = StringUtils.stripEnd(line, " ");
                    if (line.isEmpty()) {
                        line = " ";
                    }
                }
                sb.append(line).append('\n');
            }
        } finally {
            if (null != scanner) {
                scanner.close();
            }
        }

        return sb.toString();
    }

}
