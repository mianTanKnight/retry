package com.lishicloud.lretry.strategy;


import com.lishicloud.lretry.config.LishiRetryConfig;

/**
 * 立刻重试
 * @author ztq
 */
public class ImmediateRetry extends AbsRetryConfigStrategy{

    public ImmediateRetry(LishiRetryConfig config) {
        super(config);
    }

    @Override
    public long nextDuration() {
        return 0L;
    }
}
