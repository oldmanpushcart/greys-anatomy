package com.github.ompc.greys.module;

import com.alibaba.jvm.sandbox.api.http.websocket.TextMessageListener;
import com.alibaba.jvm.sandbox.api.http.websocket.WebSocketConnection;
import com.github.ompc.greys.protocol.GpSerializer;
import com.github.ompc.greys.protocol.GreysProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.ompc.greys.protocol.GpSerializer.deserialize;

public abstract class GpListener implements TextMessageListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    final public void onMessage(String gpJson) {
        try {
            onGp(deserialize(gpJson));
        } catch (Throwable cause) {
            // ignore...
        }
    }

    abstract public void onGp(final GreysProtocol<?> gp);

}
