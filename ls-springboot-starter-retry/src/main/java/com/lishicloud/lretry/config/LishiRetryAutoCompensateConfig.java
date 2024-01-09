package com.lishicloud.lretry.config;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 外部注册与默认支持
 * @author ztq
 */
@Data
@Configuration
@ConditionalOnBean(LishAutoDbConfiguration.class)
@ConditionalOnProperty(name = "retry.auto-compensate.enable", havingValue = "true")
@ConfigurationProperties(prefix = "retry.auto-compensate")
public class LishiRetryAutoCompensateConfig {

    /**
     * 延迟时间
     */
    private Integer autoTimes = 1;

    /**
     * hour
     */
    private String autoTimeUnit = "hours";

    /**
     * 立即延迟执行时间 autoTimeUnit
     */
    private Integer initialDelay = 1;

    /**
     * 自动补偿有效期
     */
    private Integer autoValidityDay  = 7;


    public TimeUnit getTimeUnit(){
        TimeUnit timeUnit;
        switch (autoTimeUnit.toLowerCase()) {
            case "days":
                timeUnit = TimeUnit.DAYS;
                break;
            case "minutes":
                timeUnit = TimeUnit.MINUTES;
                break;
            case "hours":
                timeUnit = TimeUnit.HOURS;
                break;
            default:
                throw new IllegalArgumentException("arg error!");
        }
        return timeUnit;
    }


}
