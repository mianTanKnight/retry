package com.lishicloud.lretry.strategy;


import com.lishicloud.lretry.config.LishiRetryConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * 指数间隔重试
 *
 * @author ztq
 */
@Slf4j
public class ExponentialBackoffRetry extends AbsRetryConfigStrategy {

    private final int retryInterval;
    private final int increaseTime;
    private int nextIncreaseTime;

    public ExponentialBackoffRetry(LishiRetryConfig config) {
        super(config);
        retryInterval = config.getRetryInterval();
        increaseTime = config.getIncreaseTime();
        nextIncreaseTime = increaseTime;
    }

    @Override
    public long nextDuration() {
        return retryInterval + getNextIncreaseTime();
    }

    public int getNextIncreaseTime() {
        return nextIncreaseTime += increaseTime;
    }
}
