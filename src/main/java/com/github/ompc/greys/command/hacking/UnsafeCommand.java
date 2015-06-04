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
 * Unsafe选项开启<br/>
 * 如果激活unsafe，你将可以增强到来自Bootstrap ClassLoader的类
 * Created by vlinux on 15/6/4.
 */
@Cmd(isHacking = true, named = "unsafe", desc = "Change the unsafe options")
public class UnsafeCommand implements Command {

    @IndexArg(index = 0, isRequired = false, name = "is-unsafe", summary = "let greys support unsafe options")
    private String isUnsafeString;

    @Override
    public Action getAction() {

        // 是否仅仅只是个展示
        final boolean isShow = isBlank(isUnsafeString);

        return new SilentAction() {
            @Override
            public void action(Session session, Instrumentation inst, Sender sender) throws Throwable {

                // 显示状态
                if (isShow) {
                    sender.send(true,
                            new TableView(2)
                                    .addRow("KEY", "VALUE")
                                    .addRow("isUnsafe", GlobalOptions.isUnsafe)
                                    .hasBorder(true)
                                    .padding(1).draw()
                    );
                }

                // 改变unsafe值
                else {
                    final boolean beforeIsUnsafe = GlobalOptions.isUnsafe;
                    try {
                        GlobalOptions.isUnsafe = Boolean.valueOf(isUnsafeString);
                    } catch (Throwable t) {
                        GlobalOptions.isUnsafe = false;
                    }

                    sender.send(true,
                            new TableView(3)
                                    .addRow("KEY", "BEFORE", "AFTER")
                                    .addRow("isUnsafe", beforeIsUnsafe, GlobalOptions.isUnsafe)
                                    .hasBorder(true)
                                    .padding(1).draw()
                    );

                }

            }
        };
    }

}
