package com.lishicloud.lretry.mode;



import com.lishicloud.lretry.config.LishiRetryConfig;
import com.lishicloud.lretry.strategy.ImmediateRetry;
import com.lishicloud.lretry.strategy.RetryStrategy;

import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

/**
 * @author ztq
 */
public class SyncRetryMode implements RetryMode {

    /**
     * 业务中得自行处理异常
     */
    private final Consumer<RetryStrategy> consumer;
    private final RetryStrategy retryStrategy;

    public SyncRetryMode(Consumer<RetryStrategy> consumer, LishiRetryConfig config) {
        this.consumer = consumer;
        this.retryStrategy = new ImmediateRetry(config);
    }

    public void accept() {
        while (retryStrategy.nextTimes() >= 0) {
            consumer.accept(this.retryStrategy);
            LockSupport.parkNanos(retryStrategy.nextDuration() * 1000000);
        }
    }


}
