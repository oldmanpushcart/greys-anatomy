package com.googlecode.greysanatomy.util;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

/**
 * 主机判断工具类
 * Created by vlinux on 14/10/23.
 */
public class HostUtils {

    /**
     * 判断当前主机名是否本地主机
     *
     * @param targetIp
     * @return
     */
    public static boolean isLocalHostIp(String targetIp) {

        for (String ip : getAllLocalHostIP()) {
            if (ip.equals(targetIp)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取当前主机名
     *
     * @return
     */
    public static String getLocalHostName() {
        String hostName;
        try {
            InetAddress address = InetAddress.getLocalHost();
            hostName = address.getHostName();
        } catch (Exception ex) {
            hostName = "";
        }
        return hostName;
    }

    /**
     * 获取主机名下所有网卡IP
     *
     * @return
     */
    public static Set<String> getAllLocalHostIP() {
        final Set<String> ret = new HashSet<String>();
        ret.add("127.0.0.1");
        try {
            final String hostName = getLocalHostName();
            final InetAddress[] addresses = InetAddress.getAllByName(hostName);
            for (InetAddress address : addresses) {
                final String ip = address.getHostAddress();
                if (ip.matches("^(\\d{1,3}\\.){3}\\d{1,3}$")) {
                    ret.add(address.getHostAddress());
                }
            }

        } catch (Exception ex) {
//            ret = null;
        }
        return ret;
    }

}
