package com.github.ompc.greys.core;

import com.alibaba.jvm.sandbox.api.http.printer.ConcurrentLinkedQueuePrinter;
import com.alibaba.jvm.sandbox.api.http.printer.Printer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.ompc.greys.common.protocol.Base;
import com.github.ompc.greys.core.util.UnCaughtException;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;

/**
 * JSON输出
 */
public class JsonPrinter extends ConcurrentLinkedQueuePrinter implements Printer {

    private final static ObjectMapper mapper = new ObjectMapper();

    static {

        // 设置日期格式：1985-06-12 10:58:01
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

        // JSON格式化
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        // 忽略空属性值
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

    }

    public JsonPrinter(PrintWriter writer, long delayStepTimeMs, long delayMaxTimeMs, int capacity) {
        super(writer, delayStepTimeMs, delayMaxTimeMs, capacity);
    }

    public JsonPrinter(PrintWriter writer) {
        super(writer);
    }

    // 将DTO转换为JSON，这里不会期待抛出JSON序列化失败的异常
    // 理论上这种异常都应该经过了正确的测试
    private String toJsonString(final Base base) {
        try {
            return mapper.writeValueAsString(base);
        } catch (JsonProcessingException e) {
            throw new UnCaughtException(e);
        }
    }

    /**
     * 将DTO对象转换为JSON字符串并输出{@link #print(String)}
     *
     * @param base DTO对象
     * @return JSON字符串
     */
    public JsonPrinter print(final Base base) {
        super.print(toJsonString(base));
        return this;
    }

    /**
     * 将DTO对象转换为JSON字符串并换行输出{@link #println(String)}
     *
     * @param base DTO对象
     * @return JSON字符串
     */
    public JsonPrinter println(final Base base) {
        super.println(toJsonString(base));
        return this;
    }

}
