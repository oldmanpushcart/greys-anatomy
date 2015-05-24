package com.github.ompc.greys.command;

import com.github.ompc.greys.command.affect.RowAffect;
import com.github.ompc.greys.command.annotation.Cmd;
import com.github.ompc.greys.command.annotation.NamedArg;
import com.github.ompc.greys.command.view.TableView;
import com.github.ompc.greys.server.Session;

import java.lang.instrument.Instrumentation;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import static com.github.ompc.greys.util.StringUtil.isNotBlank;
import static java.lang.String.format;

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

    @NamedArg(named = "p", hasValue = true, summary = "change the prompt of session")
    private String prompt;

    @Override
    public Action getAction() {
        return new RowAction() {

            @Override
            public RowAffect action(Session session, Instrumentation inst, Sender sender) throws Throwable {

                boolean isShow = true;
                // 设置字符集
                if (isNotBlank(charsetString)) {

                    isShow = false;

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

                }

                // 设置提示符
                if (null != prompt) {

                    isShow = false;

                    final String beforePrompt = session.getPrompt();
                    session.setPrompt(prompt);
                    sender.send(true, format("change prompt before[%s] -> new[%s]\n",
                            beforePrompt,
                            prompt));
                }

                // 展示会话状态
                if (isShow) {
                    sender.send(true, sessionToString(session));
                }

                return new RowAffect(1);
            }

        };
    }

    /*
     * 会话详情
     */
    private String sessionToString(Session gaSession) {

        return new TableView(new TableView.ColumnDefine[]{
                new TableView.ColumnDefine(TableView.Align.RIGHT),
                new TableView.ColumnDefine(TableView.Align.LEFT)
        })
                .addRow("JAVA_PID", gaSession.getJavaPid())
                .addRow("SESSION_ID", gaSession.getSessionId())
                .addRow("DURATION", gaSession.getSessionDuration())
                .addRow("CHARSET", gaSession.getCharset())
                .addRow("FROM", gaSession.getSocketChannel().socket().getRemoteSocketAddress())
                .addRow("TO", gaSession.getSocketChannel().socket().getLocalSocketAddress())
                .border(true)
                .padding(1)
                .draw();

    }

}
