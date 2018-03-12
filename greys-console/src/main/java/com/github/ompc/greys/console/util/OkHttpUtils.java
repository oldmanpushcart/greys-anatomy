package com.github.ompc.greys.console.util;

import com.github.ompc.greys.protocol.GpSerializer;
import com.github.ompc.greys.protocol.GreysProtocol;
import com.github.ompc.greys.protocol.impl.v1.Text;
import okhttp3.*;

import java.nio.charset.Charset;

import static java.lang.String.format;

public class OkHttpUtils {

    public static void main(String... args) {


        final OkHttpClient httpClient = new OkHttpClient();
        final Request request = new Request.Builder().url(
                new GaURLBuilder(
                        format("ws://%s:%s/sandbox/%s/module/websocket/greys/%s",
                                "127.0.0.1",
                                "50647",
                                "default",
                                "watch"
                        ),
                        Charset.forName("utf-8")
                )
                        .withParameter("class", "*Controller")
                        .withParameter("method", "dash*")
                        .withParameter("at", "RETURN")
                        .withParameter("watch", "returnObj")
                        .withParameter("expand", "4")
                        .withParameter("when", "returnObj != null")
                        .build()
        ).build();

        System.out.println(request.toString());

        final WebSocket webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                super.onMessage(webSocket, text);
                final GreysProtocol<?> gp = GpSerializer.deserialize(text);
                switch (gp.getType()) {
                    case TEXT:
                        System.out.println(((Text) gp.getContent()).getText());
                        break;
                    default:
                        System.out.println(text);
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                super.onClosed(webSocket, code, reason);
                System.out.println("onClosed:code=" + code + ";reason=" + reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                super.onFailure(webSocket, t, response);
                System.out.println("onFailure:cause=" + t.getMessage());
                t.printStackTrace();
            }
        });

    }


}
