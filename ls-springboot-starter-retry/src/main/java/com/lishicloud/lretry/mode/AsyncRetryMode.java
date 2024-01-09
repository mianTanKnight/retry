package com.lishicloud.lretry.mode;


import com.lishicloud.lretry.strategy.RetryStrategy;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 异步的重试模型
 *
 * @author ztq
 */
@Slf4j
public class AsyncRetryMode implements RetryMode, Runnable {

    private final Consumer<RetryStrategy> ioConsumer;
    private final RetryStrategy retryStrategy;
    //可能多个不同的线程会拥有this 使用 volatile 保证可见性
    private volatile ScheduledExecutorService executorService;

    public AsyncRetryMode(RetryStrategy retryStrategy, Consumer<RetryStrategy> ioConsumer) {
        this.ioConsumer = ioConsumer;
        this.retryStrategy = retryStrategy;
    }

    @Override
    public void run() {
        log.info("thread name: {}", Thread.currentThread().getName());
        try {
            ioConsumer.accept(this.retryStrategy);
        } finally {
            // 检查是否还需要进行下一次任务调度
            if (retryStrategy.nextTimes() > 0) {
                registerMode(executorService);
            }
        }
    }

    public void registerMode(ScheduledExecutorService scheduledExecutorService) {
        if (executorService == null) {
            executorService = scheduledExecutorService;
        }
        scheduledExecutorService.schedule(this, retryStrategy.nextDuration(), TimeUnit.MILLISECONDS);
    }
}
