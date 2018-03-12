package com.github.ompc.greys.module.resource;

import com.alibaba.jvm.sandbox.api.http.websocket.WebSocketConnection;
import com.github.ompc.greys.protocol.GpConstants;
import com.github.ompc.greys.protocol.GpSerializer;
import com.github.ompc.greys.protocol.GpType;
import com.github.ompc.greys.protocol.GreysProtocol;
import com.github.ompc.greys.protocol.impl.v1.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * GP协议书写者
 */
public class GpWriter {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final WebSocketConnection connection;

    /**
     * 构建GP协议书写者
     *
     * @param connection WebSocket connection
     */
    public GpWriter(final WebSocketConnection connection) {
        this.connection = connection;
    }

    /**
     * 写入一个GP协议
     *
     * @param gp GP协议
     * @return this
     * @throws IOException 写入失败
     */
    public GpWriter write(final GreysProtocol<?> gp) throws IOException {
        if (connection.isOpen()) {
            connection.write(GpSerializer.serialize(gp));
        } else {
            logger.debug("connection is closed, ignore this write.");
        }
        return this;
    }

    /**
     * 写入一段文本
     * <p>
     * 文本会转换为GpText协议
     *
     * @param text 文本
     * @return this
     * @throws IOException 写入失败
     */
    public GpWriter write(final String text) throws IOException {
        return write(new GreysProtocol<Text>(
                GpConstants.GP_VERSION_1_0_0,
                GpType.TEXT,
                new Text(text)
        ));
    }

    /**
     * 关闭链接
     */
    public void close() {
        if (null != connection
                && connection.isOpen()) {
            connection.disconnect();
        }
    }

}
