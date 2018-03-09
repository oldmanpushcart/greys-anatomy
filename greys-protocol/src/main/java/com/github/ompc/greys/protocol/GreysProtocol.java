package com.github.ompc.greys.protocol;

import lombok.Data;

/**
 * GP协议
 */
@Data
public class GreysProtocol<CONTENT> {

    private final String version;
    private final GpType type;
    private final CONTENT content;

    public GreysProtocol(final String version, final GpType type, final CONTENT content) {
        this.version = version;
        this.type = type;
        this.content = content;
    }

}
