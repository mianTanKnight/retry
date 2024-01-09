package com.lishicloud.lretry.manager;

import com.alibaba.fastjson.JSON;
import com.lishicloud.lretry.config.LishiRetryAutoConfiguration;
import com.lishicloud.lretry.config.LishiRetryConfig;
import com.lishicloud.lretry.exception.RetryFailInfo;
import com.lishicloud.lretry.mode.AsyncRetryMode;
import com.lishicloud.lretry.mode.RetryMode;
import com.lishicloud.lretry.mode.SyncRetryMode;
import com.lishicloud.lretry.strategy.AbsRetryConfigStrategy;
import com.lishicloud.lretry.strategy.RetryStrategy;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.apache.commons.codec.digest.DigestUtils;

import javax.annotation.PreDestroy;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 重试管理器 默认是单例(被spring 管理)
 * 对于指定方法做重试
 * 需要满足"完全"式的方法的重试(重试方法上的aop注解也需要重试)
 *
 * @author ztq
 */
@Slf4j
@Component
@Scope("singleton")
@ConditionalOnBean(LishiRetryAutoConfiguration.class)
public class LishiRetryManager implements IManager {

    private final Map<String, RetryMode> registeredMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduledExecutorService;
    private final LishiRetryJdbcManager lishiRetryJdbcManager;

    @Autowired
    public LishiRetryManager(LishiRetryConfig config, @Autowired(required = false) LishiRetryJdbcManager lishiRetryJdbcManager) {
        this.scheduledExecutorService = Executors.newScheduledThreadPool(config.getCorePoolSize(), new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Retry-Event-Pool-" + counter.getAndIncrement());
            }
        });
        this.lishiRetryJdbcManager = lishiRetryJdbcManager;
        log.info("LishiRetry init successful !!");
    }

    public boolean isRegistered(RetryEntity retry) {
        return registeredMap.containsKey(retry.getKey());
    }


    public void registerRetryEventAsync(RetryStrategy retryStrategy, RetryEntity retry) {
        Consumer<RetryStrategy> proxyConsumer = buildProxyConsumer(retry);
        AsyncRetryMode asyncRetryMode = new AsyncRetryMode(retryStrategy, proxyConsumer);
        asyncRetryMode.registerMode(scheduledExecutorService);
        registeredMap.put(retry.getKey(), asyncRetryMode);
    }

    public void registerRetryEventSync(RetryEntity retry, LishiRetryConfig config) {
        Consumer<RetryStrategy> proxyConsumer = buildProxyConsumer(retry);
        SyncRetryMode syncRetryMode = new SyncRetryMode(proxyConsumer, config);
        if (Objects.isNull(registeredMap.put(retry.getKey(), syncRetryMode))) {
            syncRetryMode.accept();
        }
    }

    public Consumer<RetryStrategy> buildProxyConsumer(RetryEntity entity) {
        return s -> {
            AbsRetryConfigStrategy absRetryConfigStrategy = (AbsRetryConfigStrategy) s;
            Object result = null;
            try {
                //执行目标代理方法
                result = absRetryConfigStrategy.retryTargetMethod(entity);
                if (result instanceof RetryFailInfo) {
                    log.info(" Retry fail Have left: [{}] , message: [{}]", absRetryConfigStrategy.getNextRetryTimes(), result);
                }
            } finally {
                if (null != lishiRetryJdbcManager) { //如果开启了落库功能
                    if (absRetryConfigStrategy.isHead()) {
                        lishiRetryJdbcManager.syncRegisterInitialRetryData(absRetryConfigStrategy, entity);
                    }
                    if (absRetryConfigStrategy.isLast()) {
                        lishiRetryJdbcManager.syncRegisterEndRetryData(entity.getKey(), result);
                        this.remover(entity.getKey());
                    }
                }
            }
        };
    }

    public void remover(String key) {
        registeredMap.remove(key);
        log.info("Manager remover : [{}]", key);
    }

    @Override
    public void init() {
        //empty impl
    }

    @Override
    @PreDestroy
    public void destroy() {
        scheduledExecutorService.shutdown();
        try {
            if (!scheduledExecutorService.awaitTermination(1, TimeUnit.MINUTES)) {
                scheduledExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutorService.shutdownNow();
        }
    }

    @Data
    public static class RetryEntity {
        /***业务key*/
        private String key;
        /***重试模式  0: 异步  1: 同步 默认异步*/
        private Byte retryMode;
        /***切入点信息*/
        private JoinPoint joinPoint;
        /*** 方法签名*/
        private MethodSignature signature;
        /*** 方法信息*/
        private Method method;
        /*** 类信息*/
        private Class<?> targetClass;
        /*** 参数信息*/
        private Object[] args;
        /*** 参数信息String*/
        private String jsonArgs;
        /*** 类名*/
        private String className;
        /*** 方法名*/
        private String methodName;

        public RetryEntity(JoinPoint joinPoint) {
            this.joinPoint = joinPoint;
            this.signature = (MethodSignature) joinPoint.getSignature();
            this.method = signature.getMethod();
            this.methodName = method.getName();
            this.targetClass = method.getDeclaringClass();
            this.className = targetClass.getName();
            this.args = joinPoint.getArgs();
            if (args.length == 1) {
                this.jsonArgs = JSON.toJSONString(args[0]);
            } else {
                this.jsonArgs = JSON.toJSONString(args);
            }
            key = DigestUtils.md5Hex(this.className + this.methodName + this.jsonArgs);
            retryMode = 0;
        }

        public void buildRetrySync() {
            this.retryMode = 1;
        }
    }
}

