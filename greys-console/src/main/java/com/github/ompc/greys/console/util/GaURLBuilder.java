package com.github.ompc.greys.console.util;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

/**
 * URL构造器
 */
public class GaURLBuilder {

    private final StringBuilder buffer = new StringBuilder();
    private final Charset charset;
    private boolean isFirst = true;

    public GaURLBuilder(Charset charset) {
        this.charset = charset;
    }

    public GaURLBuilder(String url, Charset charset) {
        this(charset);
        this.isFirst = isFirst(url);
        buffer.append(url);
    }

    private boolean isFirst(String url) {
        return StringUtils.contains(url, "?");
    }

    private void appendQuestionMarkIfFirst() {
        if (isFirst) {
            isFirst = false;
            buffer.append("?");
        } else {
            buffer.append("&");
        }
    }

    private String encode(String string) {
        try {
            return URLEncoder.encode(string, charset.name());
        } catch (UnsupportedEncodingException e) {
            return string;
        }
    }

    public GaURLBuilder withParameter(String name, String... valueArray) {
        if (ArrayUtils.isNotEmpty(valueArray)
                && StringUtils.isNotBlank(name)) {
            for (String value : valueArray) {
                if (StringUtils.isNotBlank(value)) {
                    appendQuestionMarkIfFirst();
                    buffer.append(encode(name)).append("=").append(encode(value));
                }
            }
        }
        return this;
    }

    public String build() {
        return buffer.toString();
    }

}