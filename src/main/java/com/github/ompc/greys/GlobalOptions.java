package com.github.ompc.greys;

/**
 * 全局开关
 * Created by vlinux on 15/6/4.
 */
public class GlobalOptions {

    /**
     * 是否支持Bootstrap ClassLoader的类<br/>
     * 这个开关打开之后将能代理到来自JVM的部分类，由于有非常强的安全风险可能会引起系统崩溃<br/>
     * 所以这个开关默认是关闭的，除非你非常了解你要做什么，否则请不要打开
     */
    public static volatile boolean isUnsafe = false;

    /**
     * 是否dump被增强的类<br/>
     * 这个开关打开这后，每次增强类的时候都将会将增强的类dump到文件中，以便于进行反编译分析
     */
    public static volatile boolean isDump = false;

    /**
     * dump目录<br/>
     * 当dump开关打开时候时生效
     */
    public static volatile String dumpDir = "./";

}
