package com.github.ompc.greys.command;

import com.github.ompc.greys.command.affect.EnhancerAffect;
import com.github.ompc.greys.command.affect.RowAffect;
import com.github.ompc.greys.command.annotation.Cmd;
import com.github.ompc.greys.command.annotation.IndexArg;
import com.github.ompc.greys.command.annotation.NamedArg;
import com.github.ompc.greys.server.Session;
import com.github.ompc.greys.util.Matcher;

import java.lang.instrument.Instrumentation;

import static com.github.ompc.greys.advisor.Enhancer.reset;
import static com.github.ompc.greys.util.StringUtil.EMPTY;
import static com.github.ompc.greys.util.StringUtil.isBlank;

/**
 * 恢复所有增强类<br/>
 * Created by vlinux on 15/5/29.
 */
@Cmd(named = "reset", sort = 11, desc = "Reset all the enhancer class.",
        eg = {
                "reset",
                "reset *List",
                "reset -E .*List"
        })
public class ResetCommand implements Command {

    @IndexArg(index = 0, name = "class-pattern", isRequired = false, summary = "pattern matching of classpath.classname")
    private String classPattern;

    @NamedArg(named = "E", summary = "enable the regex pattern matching")
    private boolean isRegEx = false;

    @Override
    public Action getAction() {

        // auto fix default classPattern
        if (isBlank(classPattern)) {
            classPattern = isRegEx ? ".*" : "*";
        }

        final Matcher classNameMatcher = isRegEx
                ? new Matcher.RegexMatcher(classPattern)
                : new Matcher.WildcardMatcher(classPattern);

        return new RowAction() {

            @Override
            public RowAffect action(
                    Session session,
                    Instrumentation inst,
                    Sender sender) throws Throwable {

                final EnhancerAffect enhancerAffect = reset(inst, classNameMatcher);
                sender.send(true, EMPTY);
                return new RowAffect(enhancerAffect.cCnt());
            }



        };
    }

}
