package com.github.ompc.greys.console.render;

import com.github.ompc.greys.protocol.GpType;
import com.github.ompc.greys.protocol.GreysProtocol;
import com.github.ompc.greys.protocol.impl.v1.Progress;
import com.github.ompc.greys.protocol.impl.v1.Text;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.repeat;

/**
 * GP协议渲染器
 */
public interface GpRender extends Render<GreysProtocol<?>> {

    /**
     * 默认实现的组Gp渲染器
     */
    class GroupGpRender implements GpRender {

        private final GpRender[] gpRenders;

        public GroupGpRender(GpRender... gpRenders) {
            this.gpRenders = null == gpRenders
                    ? new GpRender[0]
                    : gpRenders;
        }

        @Override
        public String rendering(GreysProtocol<?> gp) {
            final StringBuilder buffer = new StringBuilder();
            for (GpRender gpRender : gpRenders) {
                buffer.append(gpRender.rendering(gp));
            }
            return buffer.toString();
        }

    }

    class DefaultTextTypeGpRender implements GpRender {

        @Override
        public String rendering(GreysProtocol<?> gp) {
            switch (gp.getType()) {
                case TEXT:
                    return ((GreysProtocol<Text>) gp).getContent().getText();
                default:
                    return EMPTY;
            }
        }

    }

    class DefaultProgressTypeGpRender implements GpRender {

        private static final int DEFAULT_WIDTH = 30;

        private int computeRate(Progress progress) {
            return (int) (progress.getIndex() * DEFAULT_WIDTH * 1f / progress.getTotal());
        }

        @Override
        public String rendering(GreysProtocol<?> gp) {
            switch (gp.getType()) {
                case PROGRESS:
                    final Progress progress = ((GreysProtocol<Progress>) gp).getContent();
                    final int rate = computeRate(progress);
                    return format("\r%s[%-" + DEFAULT_WIDTH + "s]",
                            progress.getTitle(),
                            repeat('#', rate)
                    );
                default:
                    return EMPTY;
            }
        }

    }

    /**
     * GP渲染器构造器
     */
    class Builder {

        private final Map<GpType, GpRender> gpTypeRender
                = new LinkedHashMap<GpType, GpRender>();

        public Builder() {
            gpTypeRender.put(GpType.TEXT, new DefaultTextTypeGpRender());
            gpTypeRender.put(GpType.PROGRESS, new DefaultProgressTypeGpRender());
        }

        public Builder append(GpType gpType, GpRender gpRender) {
            gpTypeRender.put(gpType, gpRender);
            return this;
        }

        public Builder remove(GpType gpType) {
            gpTypeRender.remove(gpType);
            return this;
        }

        /**
         * 构造GP协议渲染器
         *
         * @return GP渲染器
         */
        public GpRender build() {
            return new GroupGpRender(
                    gpTypeRender.values().toArray(new GpRender[0])
            );
        }

    }


}
