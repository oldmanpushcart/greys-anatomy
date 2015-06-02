package com.github.ompc.greys.command;

import com.github.ompc.greys.command.affect.RowAffect;
import com.github.ompc.greys.command.annotation.Cmd;
import com.github.ompc.greys.command.annotation.NamedArg;
import com.github.ompc.greys.command.view.TableView;
import com.github.ompc.greys.server.Session;

import java.lang.instrument.Instrumentation;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * 查看会话状态命令
 * Created by vlinux on 15/5/3.
 */
@Cmd(named = "session", sort = 8, desc = "Show the session state.",
        eg = {
                "session",
                "session -c GBK",
                "session -c UTF-8"
        })
public class SessionCommand implements Command {

    @NamedArg(named = "c", hasValue = true, summary = "change the charset of session")
    private String charsetString;

    @Override
    public Action getAction() {
        return new RowAction() {

            @Override
            public RowAffect action(Session session, Instrumentation inst, Sender sender) throws Throwable {

                // 设置字符集
                if (isNotBlank(charsetString)) {

                    try {
                        final Charset newCharset = Charset.forName(charsetString);
                        final Charset beforeCharset = session.getCharset();
                        session.setCharset(newCharset);

                        sender.send(true, format("change charset before[%s] -> new[%s]\n",
                                beforeCharset,
                                newCharset));

                    } catch (UnsupportedCharsetException e) {
                        sender.send(true, format("unsupported charset : \"%s\"\n", charsetString));
                    }

                } else {
                    sender.send(true, sessionToString(session));
                }

                return new RowAffect(1);
            }

        };
    }

    /*
     * 会话详情
     */
    private String sessionToString(Session session) {

        return new TableView(new TableView.ColumnDefine[]{
                new TableView.ColumnDefine(TableView.Align.RIGHT),
                new TableView.ColumnDefine(TableView.Align.LEFT)
        })
                .addRow("JAVA_PID", session.getJavaPid())
                .addRow("SESSION_ID", session.getSessionId())
                .addRow("DURATION", session.getSessionDuration())
                .addRow("CHARSET", session.getCharset())
                .addRow("PROMPT", session.getPrompt())
                .addRow("FROM", session.getSocketChannel().socket().getRemoteSocketAddress())
                .addRow("TO", session.getSocketChannel().socket().getLocalSocketAddress())
                .hasBorder(true)
                .padding(1)
                .draw();

    }

}
