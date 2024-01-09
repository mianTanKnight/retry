package com.lishicloud.lretry.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 提供重试异常
 * @author ztq
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RetryException extends RuntimeException{

    private  String exceptionMessage;

    public RetryException() {
    }

    public RetryException( String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public RetryException(Throwable cause, String exceptionMessage) {
        super(cause);
        this.exceptionMessage = exceptionMessage;
    }
}
