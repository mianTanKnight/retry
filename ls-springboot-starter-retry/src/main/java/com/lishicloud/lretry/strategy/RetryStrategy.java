package com.lishicloud.lretry.strategy;


import com.lishicloud.lretry.config.LishiRetryConfig;
import com.lishicloud.lretry.manager.LishiRetryManager;

/**
 * @author ztq
 */
public interface RetryStrategy {

    /**
     * 重试策略的本质是获取下次执行的时间
     *
     * @return duration
     */
    long nextDuration();

    /**
     * 剩余几次
     *
     * @return int
     */
    int nextTimes();


    /**
     * 执行目标方法
     */
    Object retryTargetMethod(LishiRetryManager.RetryEntity retryEntity);


    /**
     * 配置文件
     */
    LishiRetryConfig getConfig();



}
