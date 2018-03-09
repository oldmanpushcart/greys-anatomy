package com.github.ompc.greys.protocol.impl.v1;

import com.github.ompc.greys.protocol.Gp;
import com.github.ompc.greys.protocol.GpType;
import lombok.Data;

@Data
@Gp(GpType.TEXT)
public class Text {

    private final String text;

}
