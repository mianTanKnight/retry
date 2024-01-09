package com.lishicloud.lretry.exception;

import com.google.common.collect.Maps;
import com.lishicloud.lretry.config.LishiRetryConfig;
import com.lishicloud.lretry.strategy.RetryStrategy;
import com.lishicloud.lretry.strategy.RetryStrategyEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 异常与策略的配置文件 支持外部覆盖
 *
 * @author ztq
 */
@Slf4j
public class ExceptionStrategyConfig {

    private final LishiRetryConfig defaultConfig;
    private final Map<RetryExceptionEnum, RetryStrategyEnum> defaultExceptionStrategy = Maps.newConcurrentMap();
    public ExceptionStrategyConfig(LishiRetryConfig config) {
        this.defaultConfig = config;
        //default init
        defaultExceptionStrategy.put(RetryExceptionEnum.BASE_EXCEPTION, RetryStrategyEnum.FIXEDINTERVAL);
        defaultExceptionStrategy.put(RetryExceptionEnum.TRANSIENT_EXCEPTION, RetryStrategyEnum.FIXEDINTERVAL);
        defaultExceptionStrategy.put(RetryExceptionEnum.SQL_EXCEPTION, RetryStrategyEnum.FULLJITTER);
    }

    public void buildExceptionStrategy(RetryExceptionEnum key, RetryStrategyEnum val) {
        //支持覆盖
        defaultExceptionStrategy.put(key, val);
    }

    public RetryStrategy getDefaultStrategy(RetryExceptionEnum key, LishiRetryConfig config) {
        return defaultExceptionStrategy.get(key).getStrategy(config == null ? defaultConfig : config);
    }

    public Map<RetryExceptionEnum, RetryStrategyEnum> getDefaultMap() {
        return defaultExceptionStrategy;
    }
}
