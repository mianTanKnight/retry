package com.lishicloud.lretry.strategy;


import com.lishicloud.lretry.config.LishiRetryConfig;
import com.lishicloud.lretry.exception.RetryFailInfo;
import com.lishicloud.lretry.manager.LishiRetryManager;
import com.lishicloud.lretry.utils.SpringBeanUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @author ztq
 */
@Slf4j
public abstract class AbsRetryConfigStrategy implements RetryStrategy {

    protected int nextRetryTimes;
    protected int initialTimes;
    protected final LishiRetryConfig config;

    public AbsRetryConfigStrategy(LishiRetryConfig config) {
        this.config = config;
        Objects.requireNonNull(config, "config must not be null");
        nextRetryTimes = config.getRetryTimes();
        initialTimes = nextRetryTimes;
    }

    @Override
    public int nextTimes() {
        return --nextRetryTimes;
    }

    @Override
    public Object retryTargetMethod(LishiRetryManager.RetryEntity retryEntity) {
        Object result;
        try {
            // 获取被代理过的目标对象
            Object bean = SpringBeanUtils.getBean(retryEntity.getTargetClass());
            // 使用反射调用方法
            Method method = bean.getClass().getMethod(retryEntity.getMethodName(), retryEntity.getMethod().getParameterTypes());
            result = method.invoke(bean, retryEntity.getArgs());
        } catch (Exception e) {
            Throwable cause = e.getCause();
            return new RetryFailInfo(e.getCause() + ":" + cause.getMessage());
        }
        return result;
    }

    public boolean isLast() {
        return nextRetryTimes == 1;
    }

    public boolean isHead() {
        return nextRetryTimes == initialTimes;
    }

    public int getNextRetryTimes() {
        return nextRetryTimes;
    }

    public int getInitialTimes(){
        return initialTimes;
    }

    @Override
    public LishiRetryConfig getConfig() {return config;}
}
