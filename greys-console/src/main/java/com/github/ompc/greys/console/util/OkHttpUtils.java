package com.github.ompc.greys.console.util;

import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

public class OkHttpUtils {

    private static volatile OkHttpClient httpClient;

    /**
     * 初始化HttpClient
     *
     * @param ip                服务器IP
     * @param port              服务器端口
     * @param connectTimeoutSec 连接超时
     * @param timeoutSec        访问超时
     */
    public synchronized void init(final String ip,
                                  final int port,
                                  final int connectTimeoutSec,
                                  final int timeoutSec) {

        if (null != httpClient) {
            throw new IllegalStateException("already init.");
        }

        httpClient = new OkHttpClient.Builder()
                .connectTimeout(connectTimeoutSec, TimeUnit.SECONDS)
                .readTimeout(timeoutSec, TimeUnit.SECONDS)
                .writeTimeout(timeoutSec, TimeUnit.SECONDS)
                .pingInterval(10, TimeUnit.SECONDS)
                .build();


    }

    private static void checkHttpClient() {
        if (null == httpClient) {
            throw new IllegalStateException("not init yet!");
        }
    }



}
