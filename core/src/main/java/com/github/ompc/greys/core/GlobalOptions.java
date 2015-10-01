package com.github.ompc.greys.core;

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
            summary = "Option to support system-level class",
            description =
                    "This option enables to proxy functionality of JVM classes. "
                            + "Due to serious security risk a JVM crash is possibly be introduced. "
                            + "Do not activate it unless you are able to manage."
    )
    public static volatile boolean isUnsafe = false;

    /**
     * 是否支持dump被增强的类<br/>
     * 这个开关打开这后，每次增强类的时候都将会将增强的类dump到文件中，以便于进行反编译分析
     */
    @Option(level = 1,
            name = "dump",
            summary = "Option to dump the enhanced classes",
            description =
                    "This option enables the enhanced classes to be dumped to external file for further de-compilation and analysis."
    )
    public static volatile boolean isDump = false;

    /**
     * 是否支持批量增强<br/>
     * 这个开关打开后，每次均是批量增强类
     */
    @Option(level = 1,
            name = "batch-re-transform",
            summary = "Option to support batch reTransform Class",
            description = "This options enables to reTransform classes with batch mode."
    )
    public static volatile boolean isBatchReTransform = true;

    /**
     * 是否支持json格式化输出<br/>
     * 这个开关打开后，使用json格式输出目标对象，配合-x参数使用
     */
    @Option(level = 2,
            name = "json-format",
            summary = "Option to support JSON format of object output",
            description = "This option enables to format object output with JSON when -x option selected."
    )
    public static volatile boolean isUsingJson = false;

//    /**
//     * 是否支持子类<br/>
//     * 这个开关打开后，默认所有的-S参数都激活
//     */
//    @Option(level = 2,
//            name = "include-sub-class",
//            summary = "Option to include subclass",
//            description = "This option defines if to include subclass."
//    )
//    public static volatile boolean isIncludeSubClass = true;

    /**
     * 是否在asm中输出
     */
    @Option(level = 1,
            name = "debug-for-asm",
            summary = "Option to print DEBUG message if ASM is involved",
            description = "This option enables to print DEBUG message of ASM for each method invocation."
    )
    public static volatile boolean isDebugForAsm = false;

    @Option(level = 1,
            name = "remove-object-methods",
            summary = "Option to remove java.lang.Object's methods when searching methods",
            description = "This option enables to remove java.lang.Object's methods when greys searching class's methods. "
                    + "Do not activate it unless you are able to manage."
    )
    public static volatile boolean isRemoveObjectMethods = true;

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
