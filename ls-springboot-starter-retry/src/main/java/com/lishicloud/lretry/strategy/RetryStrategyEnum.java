package com.lishicloud.lretry.strategy;


import com.lishicloud.lretry.config.LishiRetryConfig;

/**
 * @author ztq
 */

public enum RetryStrategyEnum {
    //立即重试
    IMMEDIATE {
        @Override
        public RetryStrategy getStrategy(LishiRetryConfig config) {
            return new ImmediateRetry(config);
        }
    },
    //间隔
    FIXEDINTERVAL {
        @Override
        public RetryStrategy getStrategy(LishiRetryConfig config) {
            return new FixedIntervalRetry(config);
        }
    },
    //指数间隔重试
    EXPONENTIALBACKOFF {
        @Override
        public RetryStrategy getStrategy(LishiRetryConfig config) {
            return new ExponentialBackoffRetry(config);
        }
    },
    //全抖动重试
    FULLJITTER {
        @Override
        public RetryStrategy getStrategy(LishiRetryConfig config) {
            return new FullJitterRetry(config);
        }
    },
    //没有重试策略
    NONE {
        @Override
        public RetryStrategy getStrategy(LishiRetryConfig config) {
            return null;
        }
    };

    public abstract RetryStrategy getStrategy(LishiRetryConfig config);

}
