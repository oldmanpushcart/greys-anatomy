package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.command.annotation.IndexArg;
import com.github.ompc.greys.core.command.annotation.NamedArg;
import com.github.ompc.greys.core.advisor.Enhancer;
import com.github.ompc.greys.core.util.affect.EnhancerAffect;
import com.github.ompc.greys.core.util.affect.RowAffect;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.util.Matcher;

import java.lang.instrument.Instrumentation;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * 恢复所有增强类<br/>
 * Created by vlinux on 15/5/29.
 */
@Cmd(name = "reset", sort = 11, summary = "Reset all the enhancer class.",
        eg = {
                "reset",
                "reset *List",
                "reset -E .*List"
        })
public class ResetCommand implements Command {

    @IndexArg(index = 0, name = "class-pattern", isRequired = false, summary = "pattern matching of classpath.classname")
    private String classPattern;

    @NamedArg(name = "E", summary = "enable the regex pattern matching")
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

                final EnhancerAffect enhancerAffect = Enhancer.reset(inst, classNameMatcher);
                sender.send(true, EMPTY);
                return new RowAffect(enhancerAffect.cCnt());
            }


        };
    }

}
