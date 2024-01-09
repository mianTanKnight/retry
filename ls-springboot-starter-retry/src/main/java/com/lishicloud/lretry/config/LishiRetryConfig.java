package com.lishicloud.lretry.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 外部注册与默认支持
 * @author ztq
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "retry.strategy")
public class LishiRetryConfig {

    /**
     * 重试次数
     */
    private Integer retryTimes = 3;
    /**
     * 重试间隔 单位毫秒
     */
    private Integer retryInterval = 1000;
    /**
     * 每次重试都会递增的时间
     * 使用 指数间隔重试策略
     */
    private Integer increaseTime = 500;

    /**
     * 范围取数的规则
     */
    private String radiusTime = "500-1000";

    /**
     * 线程池核心池大小
     */
    private Integer corePoolSize  = 2;

}
