package com.github.ompc.greys.core.message;

/**
 * 进度反馈消息
 */
public class ProgressGaMessage extends GaMessage {

    /**
     * 进度状态
     */
    public enum State {

        /**
         * 进度开始
         */
        BEGIN,

        /**
         * 进行中
         */
        PROCESSING,

        /**
         * 进度结束
         */
        FINISH
    }

    private final State state;
    private final int offset;
    private final int total;

    /**
     * 构造进度反馈
     *
     * @param state  进度状态
     * @param offset 进度偏移量(从1开始)
     * @param total  总值(进度的终点)
     */
    public ProgressGaMessage(final State state,
                             final int offset,
                             final int total) {
        super("PROCESS");
        this.state = state;
        this.offset = offset;
        this.total = total;
    }

    public State getState() {
        return state;
    }

    public int getOffset() {
        return offset;
    }

    public int getTotal() {
        return total;
    }
}