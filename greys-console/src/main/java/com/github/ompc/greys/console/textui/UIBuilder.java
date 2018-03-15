package com.github.ompc.greys.console.textui;

import org.crsh.text.*;
import org.crsh.text.ui.*;

import java.io.IOException;

import static org.crsh.text.Color.blue;
import static org.crsh.text.Color.red;
import static org.crsh.text.Color.white;
import static org.crsh.text.Style.style;
import static org.crsh.text.ui.BorderStyle.DASHED;

public class UIBuilder {

    private static final int DEFAULT_HEIGHT = 80;
    private static final int DEFAULT_WIDTH = 80;

    public static String rending(Element element) {
        return rending(DEFAULT_HEIGHT, DEFAULT_WIDTH, element);
    }

    public static String rending(int width, Element element) {
        return rending(DEFAULT_HEIGHT, width, element);
    }

    /*
     * 构建一个StringBuilder，用于渲染
     * 这里乘以一个0.2的用意是：
     *
     * 2/8原则，20%的组件才会完全渲染满height*width的内容，所以80%的时间这个空间都是浪费的
     * 这里处于GC压力考虑进行了处理
     */
    private static StringBuilder newStringBuilder(int height, int width) {
        return new StringBuilder(height * width * 2 / 10);
    }

    /**
     * 渲染Element转成文本
     *
     * @param height  渲染高度
     * @param width   渲染宽度
     * @param element UI元素
     * @return 渲染产出的文本
     */
    public static String rending(final int height, final int width, Element element) {
        final StringBuilder result = newStringBuilder(height, width);
        final LineReader renderer = element.renderer().reader(width);
        while (renderer.hasLine()) {
            final ScreenBuffer buffer = new ScreenBuffer() {
                @Override
                public void format(Format format, Appendable appendable) throws IOException {
                    format.begin(appendable);
                    Style lastStyle = null;
                    for (Object chunk : this) {
                        if (chunk instanceof Style) {
                            format.write(lastStyle = (Style) chunk, appendable);
                        } else if (chunk instanceof CLS) {
                            format.cls(appendable);
                        } else {
                            format.write((CharSequence) chunk, appendable);
                        }
                    }
                    if (lastStyle != null && lastStyle != Style.reset) {
                        format.write(Style.reset, appendable);
                    }
                    format.end(appendable);
                }
            };
            renderer.renderLine(
                    new RenderAppendable(
                            new ScreenContext() {

                                public int getWidth() {
                                    return width;
                                }

                                public int getHeight() {
                                    return height;
                                }

                                public Screenable append(CharSequence s) {
                                    buffer.append(s);
                                    return this;
                                }

                                public Appendable append(char c) throws IOException {
                                    buffer.append(c);
                                    return this;
                                }

                                public Appendable append(CharSequence csq, int start, int end) {
                                    buffer.append(csq, start, end);
                                    return this;
                                }

                                public Screenable append(Style style) {
                                    buffer.append(style);
                                    return this;
                                }

                                public Screenable cls() {
                                    buffer.cls();
                                    return this;
                                }

                                public void flush() throws IOException {
                                    buffer.flush();
                                }
                            }
                    )
            );
            final StringBuilder formatSB = new StringBuilder();
            try {
                buffer.format(Format.ANSI, formatSB);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            result.append(formatSB.toString()).append('\n');
        }
        return result.toString();
    }

    /**
     * 构建一个表格
     *
     * @param columns 表格的列宽度比例
     * @return 一个表格
     */
    public static TableElement table(int... columns) {
        return new TableElement(columns)
                .leftCellPadding(1)
                .rightCellPadding(1)
                .border(DASHED)
                .separator(DASHED);
    }

    /**
     * 构建一个树
     *
     * @param title 树标题
     * @return 树
     */
    public static TreeElement tree(String title) {
        return new TreeElement(title);
    }

    /**
     * 构建一个标签
     *
     * @param value 标签值
     * @return 标签
     */
    public static LabelElement label(String value) {
        return new LabelElement(value);
    }

    /**
     * 构建一行
     *
     * @param isHeader 是否标题行
     * @return 一行
     */
    public static RowElement row(boolean isHeader) {
        return new RowElement(isHeader);
    }

    public static RowElement row() {
        return new RowElement(false);
    }

    public static void main(String... args) {

        System.out.println(rending(
                40,
                label("vlinux")
                        .style(style().underline().bold().bg(white))
        ));
        System.out.println(rending(
                40,
                row().add(
                        label("  EMAIL").style(style().bold()),
                        label(" : ").style(style().bold()),
                        label("oldmanpush111111111111cart@gmail.com").style(style().bold().underline().fg(red))
                )
        ));
        System.out.println(rending(
                40,
                row().add(
                        label("WEBSITE").style(style().bold()),
                        label(" : ").style(style().bold()),
                        label("http://weibo.com/vlinux").style(style().bold().underline().fg(red))
                )
        ));


        System.out.println("xxx");
        System.out.println("xxx");
        System.out.println("xxx");
        System.out.println("xxx");

    }

}
