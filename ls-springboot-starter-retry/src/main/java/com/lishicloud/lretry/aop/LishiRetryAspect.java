package com.lishicloud.lretry.aop;

import com.lishicloud.lretry.annotation.Retry;
import com.lishicloud.lretry.config.LishiRetryAutoConfiguration;
import com.lishicloud.lretry.config.LishiRetryConfig;
import com.lishicloud.lretry.exception.ExceptionStrategyConfig;
import com.lishicloud.lretry.exception.RetryExceptionEnum;
import com.lishicloud.lretry.manager.LishiRetryManager;
import com.lishicloud.lretry.mode.RetryModeEnum;
import com.lishicloud.lretry.strategy.FixedIntervalRetry;
import com.lishicloud.lretry.strategy.RetryStrategy;
import com.lishicloud.lretry.strategy.RetryStrategyEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;

/**
 * @author ztq
 */
@Slf4j
@Aspect
@Component
@ConditionalOnBean(LishiRetryAutoConfiguration.class)
public class LishiRetryAspect {

    @Resource
    private LishiRetryConfig lishiRetryConfig;
    @Resource
    private LishiRetryManager lishiRetryManager;
    @Resource
    private ExceptionStrategyConfig exceptionStrategyConfig;

    @Pointcut("@annotation(com.lishicloud.lretry.annotation.Retry)")
    public void aspect() {
    }

    @Around(value = "aspect()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        LishiRetryManager.RetryEntity retryEntity = new LishiRetryManager.RetryEntity(point);
        Retry retry = retryEntity.getMethod().getAnnotation(Retry.class);
        Object proceed;
        try {
            proceed = point.proceed();
        } catch (Exception e) {
            //不管处理不处理这个错误信息都要打印出来
            log.error(e.getMessage());
            //如果已经注册 直接放弃
            if (lishiRetryManager.isRegistered(retryEntity)) {
                throw e;
            }
            RetryStrategy retryStrategy = null;
            RetryExceptionEnum retryExceptionEnum = null;
            Class<? extends Throwable>[] exceptions = retry.retryFor();
            boolean customException = isExists4Exceptions(exceptions, e);
            LishiRetryConfig retryConfig = buildCustomConfig(retry);
            //如果捕捉到自定义异常 自定义优先级最高 那么|| 的中断就会触发 retryExceptionEnum 会是 null
            if (customException || (retryExceptionEnum = isDefaultException(e)) != null) {
                retryStrategy = buildExceptionRetryStrategy(retryConfig, retry, retryExceptionEnum);
            }
            if (null == retryStrategy) {
                throw e;
            }
            if (RetryModeEnum.ASYNC == retry.retryMode()) {
                this.lishiRetryManager.registerRetryEventAsync(retryStrategy, retryEntity);
            } else {
                this.lishiRetryManager.registerRetryEventSync(retryEntity, retryStrategy.getConfig());
            }
            throw e;
        }
        return proceed;
    }

    private RetryStrategy buildExceptionRetryStrategy(LishiRetryConfig config, Retry retry, RetryExceptionEnum retryExceptionEnum) {
        RetryStrategyEnum retryStrategyEnum = retry.retryStrategy();
        //注解优先级最高
        if (RetryStrategyEnum.NONE != retryStrategyEnum) {
            return retryStrategyEnum.getStrategy(config);
        } else if (null != retryExceptionEnum) {
            return exceptionStrategyConfig.getDefaultStrategy(retryExceptionEnum, config);
        } else {
            return new FixedIntervalRetry(config);
        }
    }

    private boolean isExists4Exceptions(Class<? extends Throwable>[] exceptions, Exception e) {
        if (ArrayUtils.isEmpty(exceptions)) {
            return false;
        }
        return Arrays.stream(exceptions).anyMatch(c -> c.isInstance(e));
    }

    private LishiRetryConfig buildCustomConfig(Retry retry) {
        //注意需要new出来 不要修改源外部配置文件
        LishiRetryConfig config = new LishiRetryConfig();
        BeanUtils.copyProperties(this.lishiRetryConfig, config);
        config.setRetryTimes(retry.retryTimes());
        return config;
    }

    public RetryExceptionEnum isDefaultException(Throwable e) {
        RetryExceptionEnum[] values = RetryExceptionEnum.values();
        for (RetryExceptionEnum value : values) {
            if (value.isExists(e)) {
                return value;
            }
        }
        return null;
    }
}
