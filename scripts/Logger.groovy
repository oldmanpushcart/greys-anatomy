import com.github.ompc.greys.core.command.ScriptSupportCommand
import com.github.ompc.greys.core.Advice

import static java.lang.String.format

/**
 * 输出方法日志
 * Created by vlinux on 15/5/31.
 */
public class Logger implements ScriptSupportCommand.ScriptListener {

    @Override
    void create(ScriptSupportCommand.Output output) {
        output.println("script create.");
    }

    @Override
    void destroy(ScriptSupportCommand.Output output) {
        output.println("script destroy.");
    }

    @Override
    void before(ScriptSupportCommand.Output output, Advice advice) {
        output.println(format("before:class=%s;method=%s;",
                advice.getClazz().getSimpleName(),
                advice.getMethod().getName()))
    }

    @Override
    void afterReturning(ScriptSupportCommand.Output output, Advice advice) {
        output.println(format("returning:class=%s;method=%s;",
                advice.getClazz().getSimpleName(),
                advice.getMethod().getName()))
    }

    @Override
    void afterThrowing(ScriptSupportCommand.Output output, Advice advice) {
        output.println(format("throwing:class=%s;method=%s;",
                advice.getClazz().getSimpleName(),
                advice.getMethod().getName()))
    }
}
