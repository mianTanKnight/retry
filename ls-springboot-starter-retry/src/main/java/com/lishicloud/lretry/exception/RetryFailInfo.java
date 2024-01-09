package com.lishicloud.lretry.exception;

import lombok.Data;


/**
 * @author ztq
 */
@Data
public class RetryFailInfo {
    private final String message;

    public RetryFailInfo(String message) {
        this.message = message;
    }
}
