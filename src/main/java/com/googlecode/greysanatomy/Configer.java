package com.googlecode.greysanatomy;

import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.googlecode.greysanatomy.util.GaReflectUtils.*;
import static com.googlecode.greysanatomy.util.GaStringUtils.*;

/**
 * 配置类
 *
 * @author vlinux
 */
public class Configer {

    private String targetIp;                //目标主机IP
    private int targetPort;                 //目标进程号
    private int javaPid;                    //对方java进程号
    private long connectTimeout = 6000;     //连接超时时间(ms)
    private boolean multi;                  //多用户模式
    private String consolePrompt = "ga?>";  //控制台提示符

    /**
     * 将Configer对象转换为字符串
     */
    public String toString() {
        final StringBuilder strSB = new StringBuilder();
        for (Field field : getFields(Configer.class)) {
            try {
                strSB.append(field.getName()).append("=").append(encode(newString(getFieldValueByField(this, field)))).append(";");
            } catch (Throwable t) {
                //
            }
        }//for
        return strSB.toString();
    }

    /**
     * 将toString的内容转换为Configer对象
     *
     * @param toString
     * @return
     */
    public static Configer toConfiger(String toString) {
        final Configer configer = new Configer();
        final String[] pvs = StringUtils.split(toString, ";");
        for (String pv : pvs) {
            try {
                final String[] strs = StringUtils.split(pv, "=");
                final String p = strs[0];
                final String v = decode(strs[1]);
                final Field field = getField(Configer.class, p);
                set(field, valueOf(field.getType(), v), configer);
            } catch (Throwable t) {
                //
            }
        }
        return configer;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(int targetPort) {
        this.targetPort = targetPort;
    }

    public int getJavaPid() {
        return javaPid;
    }

    public void setJavaPid(int javaPid) {
        this.javaPid = javaPid;
    }

    public long getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public boolean isMulti() {
        return multi;
    }

    public void setMulti(boolean multi) {
        this.multi = multi;
    }

    public String getConsolePrompt() {
        return consolePrompt;
    }

    public void setConsolePrompt(String consolePrompt) {
        this.consolePrompt = consolePrompt;
    }

    public static void main(String... args) throws UnknownHostException {
        System.out.println(InetAddress.getLocalHost().toString());
    }

}
