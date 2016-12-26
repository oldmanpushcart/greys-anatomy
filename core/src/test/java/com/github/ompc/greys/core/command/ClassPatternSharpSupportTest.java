package com.github.ompc.greys.core.command;

import com.github.ompc.greys.core.util.GaReflectUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * .
 * <p/>
 *
 * @author <a href="mailto:yunie.yx@cainiao.com">yunie.yx</a>
 * @version 1.0.0
 * @since 2016-12-26
 */
public class ClassPatternSharpSupportTest {

    @Test
    public void testWatch() throws Exception {
        Command command = Commands.getInstance().newCommand("watch org.apache.commons.lang3.StringUtils isBlank params[0]");
        Assert.assertEquals("org.apache.commons.lang3.StringUtils", getFieldValue(command, WatchCommand.class, "classPattern"));
        Assert.assertEquals("isBlank", getFieldValue(command, WatchCommand.class, "methodPattern"));

        final Command withSharpCommand = Commands.getInstance().newCommand("watch org.apache.commons.lang3.StringUtils#isBlank params[0]");
        Assert.assertEquals("org.apache.commons.lang3.StringUtils", getFieldValue(withSharpCommand, WatchCommand.class, "classPattern"));
        Assert.assertEquals("isBlank", getFieldValue(withSharpCommand, WatchCommand.class, "methodPattern"));
    }

    private static String getFieldValue(Object command, Class commandClass, String name) throws Exception {
        return GaReflectUtils.getValue(command, GaReflectUtils.getField(commandClass, name));
    }

    public static void main(String[] args) throws InterruptedException {
        int i = 0;
        while (i++ < 100) {
            StringUtils.isBlank("hello world " + i);
            Thread.sleep(2000);
        }
    }
}
