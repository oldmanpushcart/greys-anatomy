package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.advisor.Enhancer;
import com.github.ompc.greys.core.command.annotation.Cmd;
import com.github.ompc.greys.core.server.Session;
import com.github.ompc.greys.core.util.LogUtil;
import com.github.ompc.greys.core.util.Matcher;
import com.github.ompc.greys.core.util.SearchUtils;
import com.github.ompc.greys.core.util.affect.EnhancerAffect;
import com.github.ompc.greys.core.util.affect.RowAffect;
import org.slf4j.Logger;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.apache.commons.lang3.reflect.FieldUtils.getField;

/**
 * 关闭命令
 * Created by oldmanpushcart@gmail.com on 14/10/23.
 */
@Cmd(name = "shutdown", sort = 11, summary = "Shut down Greys server and exit the console",
        eg = {
                "shutdown"
        })
public class ShutdownCommand implements Command {

    private final Logger logger = LogUtil.getLogger();

    /*
     * 从GreysClassLoader中加载Spy
     */
    private Class<?> loadSpyClassFromGreysClassLoader(final ClassLoader greysClassLoader, final String spyClassName) {
        try {
            return greysClassLoader.loadClass(spyClassName);
        } catch (ClassNotFoundException e) {
            logger.warn("Spy load failed from GreysClassLoader, that is impossible!", e);
            return null;
        }
    }

    /*
     * 重置agent的greys
     * 让下载重新加载greys的时候能重新初始化ClassLoader
     */
    private void reset() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        // 从GreysClassLoader中加载Spy
        final Class<?> spyClassFromGreysClassLoader = loadSpyClassFromGreysClassLoader(
                ShutdownCommand.class.getClassLoader(),
                "com.github.ompc.greys.agent.Spy"
        );
        if (null != spyClassFromGreysClassLoader) {

            // 重置整个greys
            final Method agentResetMethod = (Method) getField(spyClassFromGreysClassLoader, "AGENT_RESET_METHOD").get(null);
            agentResetMethod.invoke(null);

        }
    }

    /*
     * 重置所有已经加载到JVM的Spy
     */
    private void cleanSpy(final Instrumentation inst) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        for (Class spyClass : SearchUtils.searchClass(inst, new Matcher.PatternMatcher(false, "com.github.ompc.greys.agent.Spy"))) {
            final Method cleanMethod = spyClass.getMethod("clean");
            cleanMethod.invoke(null);
        }

    }

    @Override
    public Action getAction() {
        return new RowAction() {
            @Override
            public RowAffect action(Session session, Instrumentation inst, Printer printer) throws Throwable {

                // 退出之前需要重置所有的增强类
                // 重置之前增强的类
                final EnhancerAffect enhancerAffect = Enhancer.reset(inst);

                // reset for agent ClassLoader
                reset();

                // cleanSpy the spy
                cleanSpy(inst);

                printer.println("Greys Server is shut down.").finish();
                return new RowAffect(enhancerAffect.cCnt());
            }

        };
    }

}
