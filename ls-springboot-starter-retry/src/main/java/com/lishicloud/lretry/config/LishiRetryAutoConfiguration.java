package com.lishicloud.lretry.config;

import com.lishicloud.lretry.exception.ExceptionStrategyConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import javax.sql.DataSource;

/**
 * @author ztq
 */
@Slf4j
@Configuration
@ComponentScan("com.lishicloud.lretry")
public class LishiRetryAutoConfiguration {

    @Resource
    private DataSource dataSource;

    @Resource
    private LishiRetryConfig lishiRetryConfig;

    @Bean
    public JdbcTemplate jdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean(ExceptionStrategyConfig.class)
    public ExceptionStrategyConfig exceptionStrategyConfig() {
        return new ExceptionStrategyConfig(lishiRetryConfig);
    }


}
