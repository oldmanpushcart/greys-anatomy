package com.github.ompc.greys;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 全局开关
 * Created by vlinux on 15/6/4.
 */
public class GlobalOptions {

    /**
     * 是否支持系统类<br/>
     * 这个开关打开之后将能代理到来自JVM的部分类，由于有非常强的安全风险可能会引起系统崩溃<br/>
     * 所以这个开关默认是关闭的，除非你非常了解你要做什么，否则请不要打开
     */
    @Option(level = 0,
            name = "unsafe",
            summary = "is support system class",
            description =
                    "After this option is activated, the class will be able to come from the JVM class. "
                            + "Because of a very strong security risk can cause a system crash, so the switch is turned off by default. "
                            + "Please don't activate unless you know what you need to do."
    )
    public static volatile boolean isUnsafe = false;

    /**
     * 是否支持dump被增强的类<br/>
     * 这个开关打开这后，每次增强类的时候都将会将增强的类dump到文件中，以便于进行反编译分析
     */
    @Option(level = 1,
            name = "dump",
            summary = "is support dump the enhance class",
            description =
                    "After this option is activated, each time the enhanced class will be dump to the file, to facilitate the reverse compiler analysis."
    )
    public static volatile boolean isDump = false;

    /**
     * 选项
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Option {

        /*
         * 选项级别，数字越小级别越高
         */
        int level();

        /*
         * 选项名称
         */
        String name();

        /*
         * 选项摘要说明
         */
        String summary();

        /*
         * 命令描述
         */
        String description();

    }

}
