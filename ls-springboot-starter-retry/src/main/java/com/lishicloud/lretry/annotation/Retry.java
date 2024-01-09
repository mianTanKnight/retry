package com.lishicloud.lretry.annotation;


import com.lishicloud.lretry.mode.RetryModeEnum;
import com.lishicloud.lretry.strategy.RetryStrategyEnum;

import java.lang.annotation.*;

/**
 * <p>
 * retry 需要使用者保证被重试的接口幂等性与原子性
 * 1: 提供异步和同步的两种方式
 * 2: 提供多种重试策略组合(RetryStrategyEnum)
 * 3: 重试记录落库(JdbcTemplate)
 * 4: 支持自动补偿 (ReTryAutoCompensateManager)
 * <p>
 * 重试功能针对与执行出现错误的接口
 * 如果执行错误了 那么原本想返回的结果是无法返回的(被重试的接口有return)
 * 但如果重试成功 异步模式是无法返回结果 同步支持
 * @author ztq
 */
@Documented
@Target(value = ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Retry {

    /**
     * 重试模型
     * Async: 不会阻塞调用线程 会注册到后台使用threadPool尝试重试
     * Sync: 会阻塞调用线程 尝试重试
     */
    RetryModeEnum retryMode() default RetryModeEnum.ASYNC;

    /**
     * 重试异常 如果列表为空，会尝试匹配默认支持的异常
     * 如果不需要重试功能 请去掉@Retry
     */
    Class<? extends Throwable>[] retryFor() default {};

    /**
     * 如果没有配置 就走默认的最平滑的重试策略
     * 注意:如果异常未击中 是直接放弃重试 重试策略也无意义
     */
    RetryStrategyEnum retryStrategy() default RetryStrategyEnum.NONE;

    /**
     * 重试次数
     */
    int retryTimes() default 3;
}
