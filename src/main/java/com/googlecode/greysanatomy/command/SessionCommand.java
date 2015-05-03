package com.googlecode.greysanatomy.command;

import com.googlecode.greysanatomy.command.annotation.Cmd;
import com.googlecode.greysanatomy.command.annotation.NamedArg;
import com.googlecode.greysanatomy.server.GaSession;
import com.googlecode.greysanatomy.util.GaStringUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

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
public class SessionCommand extends Command {

    @NamedArg(named = "c", hasValue = true, description = "change the charset of session")
    private String charsetString;

    @Override
    public Action getAction() {
        return new Action() {

            @Override
            public void action(final GaSession gaSession, final Info info, final Sender sender) throws Throwable {

                // 如果是设置字符集
                if (GaStringUtils.isNotBlank(charsetString)) {

                    try {
                        final Charset newCharset = Charset.forName(charsetString);
                        final Charset beforeCharset = gaSession.getCharset();
                        gaSession.setCharset(newCharset);

                        sender.send(true, format("change charset before[%s] -> new[%s]",
                                beforeCharset,
                                newCharset));

                    } catch(UnsupportedCharsetException e) {
                        sender.send(true, format("unsupported charset : \"%s\"", charsetString));
                    }


                }

                // 展示会话状态
                else {
                    sender.send(true, sessionToString(gaSession));
                }

            }

        };
    }

    /*
     * 会话详情
     */
    private String sessionToString(GaSession gaSession) {

        final StringBuilder sessionSB = new StringBuilder();

        sessionSB.append(
                format("javaPid=%s;sessionId=%s;duration=%s;charset=%s;",
                        gaSession.getJavaPid(),
                        gaSession.getSessionId(),
                        gaSession.getSessionDuration(),
                        gaSession.getCharset().displayName()));

        try {
            sessionSB.append("from=").append(gaSession.getSocketChannel().getRemoteAddress()).append(";");
            sessionSB.append("to=").append(gaSession.getSocketChannel().getLocalAddress()).append(";");
        } catch (IOException ioe) {
            // ignore
        }


        return sessionSB.toString();

    }

}
