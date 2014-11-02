package com.googlecode.greysanatomy.console.command;

import com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer.TransformResult;
import com.googlecode.greysanatomy.console.command.annotation.RiscCmd;
import com.googlecode.greysanatomy.console.command.annotation.RiscIndexArg;
import com.googlecode.greysanatomy.console.command.annotation.RiscNamedArg;
import com.googlecode.greysanatomy.console.server.ConsoleServer;
import com.googlecode.greysanatomy.probe.Advice;
import com.googlecode.greysanatomy.probe.AdviceListenerAdapter;
import com.googlecode.greysanatomy.util.GaStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;

import static com.googlecode.greysanatomy.agent.GreysAnatomyClassFileTransformer.transform;
import static com.googlecode.greysanatomy.console.server.SessionJobsHolder.registJob;
import static com.googlecode.greysanatomy.probe.ProbeJobs.activeJob;

/**
 * javascript语言增强
 *
 * @author vlinux
 */
@RiscCmd(named = "js", sort = 3, desc = "Let Greys use the JavaScript enhancement.",
eg={
        "js -f /tmp/debug.js org\\.apache\\.commons\\.lang\\.StringUtils isEmpty",
        "js -f /tmp/debug.js .*StringUtils isEmpty",
})
public class JavaScriptCommand extends Command {

    private static final Logger logger = LoggerFactory.getLogger("greysanatomy");

    @RiscIndexArg(index = 0, name = "class-regex", description = "regex match of classpath.classname")
    private String classRegex;

    @RiscIndexArg(index = 1, name = "method-regex", description = "regex match of methodname")
    private String methodRegex;

    @RiscNamedArg(named = "f", hasValue = true, description = "the file of javascript")
    private File scriptFile;

    /**
     * TLS = ThreadLocals
     *
     * @author vlinux
     */
    public static class TLS {

        /**
         * JLS中保存的TLS对象的key
         */
        public static final String TLS_JLSKEY = "greys-TLS" + (char) 29;

        private final ThreadLocal<Map<String, Object>> tls = new ThreadLocal<Map<String, Object>>();

        public void put(String name, Object value) {
            if (tls.get() == null) {
                tls.set(new HashMap<String, Object>());
            }
            tls.get().put(name, value);
        }

        public Object get(String name) {
            if (tls.get() == null) {
                return null;
            }
            return tls.get().get(name);
        }

    }

    /**
     * JobLocals
     * 每个jobKill的时候清掉
     *
     * @author chengtongda
     */
    public static class JLS {

        private static final Map<String, Map<String, Object>> jobLocals = new HashMap<String, Map<String, Object>>();

        public static Map<String, Object> getJLS(String jobId) {
            if (jobLocals.get(jobId) == null) {
                jobLocals.put(jobId, new HashMap<String, Object>());
            }
            return jobLocals.get(jobId);
        }

        public static void removeJob(String jobId) {
            jobLocals.remove(jobId);
        }

        public static void put(String jobId, String key, Object value) {
            if (jobLocals.get(jobId) == null) {
                jobLocals.put(jobId, new HashMap<String, Object>());
            }
            jobLocals.get(jobId).put(key, value);
        }

        public static Object get(String jobId, String key) {
            if (jobLocals.get(jobId) == null) {
                return null;
            }
            return jobLocals.get(jobId).get(key);
        }
    }

    /**
     * 给脚本使用的output，用以输出信息到ga-console-client
     *
     * @author vlinux
     */
    public static class Output {

        private final Sender sender;

        public Output(Sender sender) {
            this.sender = sender;
        }

        public void println(String msg) {
            sender.send(false, msg);
        }

    }

    /**
     * 脚本实现接口
     *
     * @author vlinux
     */
    public static interface ScriptListener {

        void before(Advice p, Output output, Map<String, Object> jls, TLS tls);

        void success(Advice p, Output output, Map<String, Object> jls, TLS tls);

        void exception(Advice p, Output output, Map<String, Object> jls, TLS tls);

        void finished(Advice p, Output output, Map<String, Object> jls, TLS tls);

        void create(Output output, Map<String, Object> jls, TLS tls);

        void destroy(Output output, Map<String, Object> jls, TLS tls);

    }

    @Override
    public Action getAction() {
        return new Action() {

            @Override
            public void action(final ConsoleServer consoleServer, final Info info, final Sender sender) throws Throwable {

                if (!scriptFile.isFile()
                        || !scriptFile.exists()
                        || !scriptFile.canRead()) {
                    sender.send(true, "script file not exist.");
                    return;
                }

                JLS.put(info.getJobId(), TLS.TLS_JLSKEY, new TLS());
                final Output output = new Output(sender);
                final ScriptEngine jsEngine = new ScriptEngineManager().getEngineByExtension("js");
                final Invocable invoke = (Invocable) jsEngine;
                final ScriptListener scriptListener;
                try {
                    jsEngine.eval("var $field=com.googlecode.greysanatomy.util.GaReflectUtils.getFieldValueByFieldName;");
                    jsEngine.eval("var $jstack=com.googlecode.greysanatomy.util.GaReflectUtils.jstack;");
                    jsEngine.eval(new FileReader(scriptFile));
                    scriptListener = invoke.getInterface(ScriptListener.class);
                } catch (FileNotFoundException e) {
                    final String msg = "script file not exist.";
                    logger.warn(msg, e);
                    sender.send(true, msg);
                    return;
                } catch (ScriptException e) {
                    final String msg = "script execute failed." + e.getMessage();
                    logger.warn(msg, e);
                    sender.send(true, msg);
                    return;
                }

                final Instrumentation inst = info.getInst();
                final TransformResult result = transform(inst, classRegex, methodRegex, new AdviceListenerAdapter() {

                    @Override
                    public void onBefore(final Advice p) {
                        try {
                            scriptListener.before(p, output, JLS.getJLS(info.getJobId()), (TLS) JLS.get(info.getJobId(), TLS.TLS_JLSKEY));
                        } catch (Throwable t) {
                            output.println(t.getMessage());
                        }
                    }

                    @Override
                    public void onSuccess(final Advice p) {
                        try {
                            scriptListener.success(p, output, JLS.getJLS(info.getJobId()), (TLS) JLS.get(info.getJobId(), TLS.TLS_JLSKEY));
                        } catch (Throwable t) {
                            output.println(t.getMessage());
                        }
                    }

                    @Override
                    public void onException(final Advice p) {
                        try {
                            scriptListener.exception(p, output, JLS.getJLS(info.getJobId()), (TLS) JLS.get(info.getJobId(), TLS.TLS_JLSKEY));
                        } catch (Throwable t) {
                            output.println(t.getMessage());
                        }
                    }

                    @Override
                    public void onFinish(final Advice p) {
                        try {
                            scriptListener.finished(p, output, JLS.getJLS(info.getJobId()), (TLS) JLS.get(info.getJobId(), TLS.TLS_JLSKEY));
                        } catch (Throwable t) {
                            output.println(t.getMessage());
                        }
                    }

                    @Override
                    public void create() {
                        try {
                            scriptListener.create(output, JLS.getJLS(info.getJobId()), (TLS) JLS.get(info.getJobId(), TLS.TLS_JLSKEY));
                        } catch (Throwable t) {
                            output.println(t.getMessage());
                        }
                    }

                    @Override
                    public void destroy() {
                        try {
                            scriptListener.destroy(output, JLS.getJLS(info.getJobId()), (TLS) JLS.get(info.getJobId(), TLS.TLS_JLSKEY));
                        } catch (Throwable t) {
                            output.println(t.getMessage());
                        }
                    }

                }, info);

                // 注册任务
                registJob(info.getSessionId(), result.getId());

                // 激活任务
                activeJob(result.getId());

                final StringBuilder message = new StringBuilder();
                message.append(GaStringUtils.LINE);
                message.append(String.format("done. probe:c-Cnt=%s,m-Cnt=%s\n",
                        result.getModifiedClasses().size(),
                        result.getModifiedBehaviors().size()));
                message.append(GaStringUtils.ABORT_MSG).append("\n");
                sender.send(false, message.toString());
            }

        };
    }

}
