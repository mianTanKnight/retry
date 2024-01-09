package com.lishicloud.lretry.strategy;


import com.lishicloud.lretry.config.LishiRetryConfig;
import lombok.ToString;


/**
 * 固定间隔重试
 * @author ztq
 */
@ToString
public class FixedIntervalRetry extends AbsRetryConfigStrategy {

    private final int retryInterval;

    public FixedIntervalRetry(LishiRetryConfig config) {
        super(config);
        retryInterval = config.getRetryInterval();
    }

    @Override
    public long nextDuration() {
        return retryInterval;
    }
}
