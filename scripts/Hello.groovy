import com.github.ompc.greys.command.ScriptSupportCommand

/**
 * 输出一个简单的Hello World!
 * Created by vlinux on 15/6/1.
 */
public class Hello extends ScriptSupportCommand.ScriptListenerAdapter {

    @Override
    void create(ScriptSupportCommand.Output output) {
        output.println("Hello World!").finish();
    }

}