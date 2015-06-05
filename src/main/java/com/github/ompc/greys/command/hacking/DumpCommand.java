package com.github.ompc.greys.command.hacking;

import com.github.ompc.greys.GlobalOptions;
import com.github.ompc.greys.command.Command;
import com.github.ompc.greys.command.annotation.Cmd;
import com.github.ompc.greys.command.annotation.IndexArg;
import com.github.ompc.greys.command.view.TableView;
import com.github.ompc.greys.server.Session;

import java.lang.instrument.Instrumentation;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Dump命令
 * Created by vlinux on 15/6/2.
 */
@Cmd(isHacking = true, named = "dump", desc = "Change the dump options")
public class DumpCommand implements Command {

    @IndexArg(index = 0, isRequired = false, name = "is-dump", summary = "let greys support dump options")
    private String isDumpString;

    @Override
    public Action getAction() {

        // 是否仅仅只是个展示
        final boolean isShow = isBlank(isDumpString);

        return new SilentAction() {
            @Override
            public void action(Session session, Instrumentation inst, Sender sender) throws Throwable {

                // 显示状态
                if (isShow) {
                    sender.send(true,
                            new TableView(2)
                                    .addRow("KEY", "VALUE")
                                    .addRow("isDump", GlobalOptions.isDump)
                                    .hasBorder(true)
                                    .padding(1).draw()
                    );
                }

                // 改变unsafe值
                else {
                    final boolean beforeIsUnsafe = GlobalOptions.isDump;
                    try {
                        GlobalOptions.isDump = Boolean.valueOf(isDumpString);
                    } catch (Throwable t) {
                        GlobalOptions.isDump = false;
                    }

                    sender.send(true,
                            new TableView(3)
                                    .addRow("KEY", "BEFORE", "AFTER")
                                    .addRow("isDump", beforeIsUnsafe, GlobalOptions.isDump)
                                    .hasBorder(true)
                                    .padding(1).draw()
                    );

                }

            }
        };
    }

}
