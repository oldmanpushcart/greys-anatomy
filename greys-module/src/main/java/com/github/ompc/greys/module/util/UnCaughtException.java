package com.github.ompc.greys.module.util;

/**
 * 未捕获异常<br/>
 * 用来封装不希望抛出的异常
 *
 * @author oldmanpushcart@gmail.com
 */
public class UnCaughtException extends RuntimeException {

    public UnCaughtException(Throwable cause) {
        super(cause);
    }
}