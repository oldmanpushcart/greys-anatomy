package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.textui.TTable;
import com.github.ompc.greys.core.util.affect.RowAffect;

import java.lang.instrument.Instrumentation;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * 查看会话状态命令
 * Created by oldmanpushcart@gmail.com on 15/5/3.
 */
@Cmd(name = "session", sort = 8, summary = "Display current session information",
        eg = {
                "session",
                "session -c GBK",
                "session -c UTF-8",
                "session -s true"
        })
public class SessionCommand implements Command {

    @NamedArg(name = "c", hasValue = true, summary = "Modify the character of session")
    private String charsetString;

    @NamedArg(name = "s", hasValue = true, summary = "Modify the silent of session")
    private Boolean silent;

    @Override
    public Action getAction() {
        return new RowAction() {

            @Override
            public RowAffect action(Session session, Instrumentation inst, Printer printer) throws Throwable {

                // 设置字符集
                if (isNotBlank(charsetString)) {

                    try {
                        final Charset newCharset = Charset.forName(charsetString);
                        final Charset beforeCharset = session.getCharset();
                        session.setCharset(newCharset);

                        printer.println(format("Character setValue is modified. [%s] -> [%s]",
                                beforeCharset,
                                newCharset))
                                .finish();

                    } catch (UnsupportedCharsetException e) {
                        printer.println(format("Desupported character setValue : \"%s\"", charsetString)).finish();
                    }

                } else {
                    printer.print(sessionToString(session)).finish();
                }

                // 设置会话静默
                if (null != silent) {
                    final boolean beforeSilent = session.isSilent();
                    session.setSilent(silent);
                    printer.println(format("Silent setValue is modified. [%s] -> [%s]",
                            beforeSilent,
                            session.isSilent()))
                            .finish();
                }

                return new RowAffect(1);
            }

        };
    }

    /*
     * 会话详情
     */
    private String sessionToString(Session session) {

        return new TTable(new TTable.ColumnDefine[]{
                new TTable.ColumnDefine(TTable.Align.RIGHT),
                new TTable.ColumnDefine(TTable.Align.LEFT)
        })
                .addRow("JAVA_PID", session.getJavaPid())
                .addRow("SESSION_ID", session.getSessionId())
                .addRow("DURATION", session.getSessionDuration())
                .addRow("SILENT", session.isSilent())
                .addRow("CHARSET", session.getCharset())
                .addRow("PROMPT", session.getPrompt())
                .addRow("FROM", session.getSocketChannel().socket().getRemoteSocketAddress())
                .addRow("TO", session.getSocketChannel().socket().getLocalSocketAddress())
                .padding(1)
                .rendering();

    }

}
