package com.lishicloud.lretry.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * @author admin
 */
@Configuration
@ConditionalOnProperty(name = "retry.auto-db.enable", havingValue = "true")
public class LishAutoDbConfiguration {
}
