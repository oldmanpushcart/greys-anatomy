package com.github.ompc.greys.protocol.impl.v1;

import com.github.ompc.greys.protocol.Gp;
import com.github.ompc.greys.protocol.GpType;
import lombok.Data;

@Data
@Gp(GpType.PROGRESS)
public class Progress {

    private final String title;
    private final boolean isBegin;
    private final boolean isEnd;
    private final int total;
    private final int index;
    private final String targetClassName;
    private final boolean isError;
    private final String errorMessage;

}
