package com.lishicloud.lretry.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lishicloud.lretry.config.LishiRetryAutoCompensateConfig;
import com.lishicloud.lretry.constant.RetryConstant;
import com.lishicloud.lretry.utils.SpringBeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Scope;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 必须要手动开启auto-compensate
 *
 * @author ztq
 */
@Slf4j
@Scope("singleton")
@Component
@ConditionalOnBean(LishiRetryAutoCompensateConfig.class)
public class LishiReTryAutoCompensateManager implements IManager {
    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 单定时线程 排队执行
     */
    private final ScheduledExecutorService autoCompensatePool;
    private final LishiRetryAutoCompensateConfig lishiRetryAutoCompensateConfig;


    @Autowired
    public LishiReTryAutoCompensateManager(LishiRetryAutoCompensateConfig retryAutoCompensateConfig) {
        lishiRetryAutoCompensateConfig = retryAutoCompensateConfig;
        autoCompensatePool = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "Auto-Compensate-Pool-" + counter.getAndIncrement());
            }
        });
        log.info("AutoCompensateManager register successful ! config :[{}]", retryAutoCompensateConfig);
    }

    /**
     * AutoCompensate 的具体实现
     */
    public static class AutoCompensateRetry implements Runnable {
        private final int autoValidityDay;
        private final JdbcTemplate jdbcTemplate;

        public AutoCompensateRetry(int autoValidityDay, JdbcTemplate jdbcTemplate) {
            this.autoValidityDay = autoValidityDay;
            this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        public void run() {
            List<LishiRetryJdbcManager.ReTryInfoEntity> autoCompensateInfos = null;
            try {
                autoCompensateInfos = getAutoCompensateInfos();
                log.info("AutoCompensate Message Data : [{}]",autoCompensateInfos);
                for (LishiRetryJdbcManager.ReTryInfoEntity autoCompensateInfo : autoCompensateInfos) {
                    try {
                        callTargetMethod(autoCompensateInfo);
                        autoCompensateInfo.setDataStatus(RetryConstant.RETRY_SUCCESSFUL);
                        log.info("[{}] autoCompensate Successful!", autoCompensateInfo);
                    } catch (Exception e) {
                        handleException(e,autoCompensateInfo);
                    }
                    autoCompensateInfo.setAutoCompensate(RetryConstant.AUTO_COMPENSATE_ED);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if(autoCompensateInfos != null) {
                    updateAutoCompensateInfos(autoCompensateInfos);
                }
            }
        }

        private void handleException(Exception e, LishiRetryJdbcManager.ReTryInfoEntity autoCompensateInfo) {
            autoCompensateInfo.setDataStatus(RetryConstant.RETRY_FAIL);
            String resultStr = e.getCause() + ":" + e.getMessage();
            resultStr = resultStr.length() > RetryConstant.MAX_MESSAGE_LENGTH ? resultStr.substring(0, RetryConstant.MAX_MESSAGE_LENGTH) : resultStr;
            autoCompensateInfo.setResultMsg(resultStr);
            log.info("[{}] autoCompensate Fail!", autoCompensateInfo);
        }

        private void callTargetMethod(LishiRetryJdbcManager.ReTryInfoEntity reTryInfoEntity) throws Exception {
            Class<?> clazz = Class.forName(reTryInfoEntity.getClassName());
            Object bean = SpringBeanUtils.getBean(clazz);
            Class<?> paramType = Class.forName(reTryInfoEntity.getReqArgsType());
            Object param = new ObjectMapper().readValue(reTryInfoEntity.getReqArgs(), paramType);
            // 调用目标方法
            Method method = clazz.getMethod(reTryInfoEntity.getMethodName(), paramType);
            method.invoke(bean, param);
        }

        private void updateAutoCompensateInfos(List<LishiRetryJdbcManager.ReTryInfoEntity> autoCompensateInfos) {
            String sql = "UPDATE retry_event_compensation SET dataStatus = ?, autoCompensate = ? WHERE id = ?";
            JdbcTemplate jdbcTemplate = SpringBeanUtils.getBean(JdbcTemplate.class);
            List<Object[]> batchArgs = new ArrayList<>();
            for (LishiRetryJdbcManager.ReTryInfoEntity autoCompensateInfo : autoCompensateInfos) {
                Object[] values = new Object[]{
                        autoCompensateInfo.getDataStatus(),
                        autoCompensateInfo.getAutoCompensate(),
                        autoCompensateInfo.getId()
                };
                batchArgs.add(values);
            }
            try {
                //批量更新
                jdbcTemplate.batchUpdate(sql, batchArgs);
            } catch (DataAccessException e) {
                e.printStackTrace();
            }
        }

        private List<LishiRetryJdbcManager.ReTryInfoEntity> getAutoCompensateInfos() {
            List<LishiRetryJdbcManager.ReTryInfoEntity> query = null;
            try {
                Date now = new Date();
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(now);
                calendar.add(Calendar.DAY_OF_YEAR, -autoValidityDay);
                Date oneWeekAgo = calendar.getTime();
                String sql = "SELECT * FROM retry_event_compensation WHERE autoCompensate = 1 AND retryCreate BETWEEN ? AND ?";
                query = jdbcTemplate.query(sql, new Object[]{oneWeekAgo, now}, new LishiRetryJdbcManager.ReTryInfoEntityMapper());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return query;
        }
    }


    @Override
    @PostConstruct
    public void init() {
        autoCompensatePool.scheduleAtFixedRate(new AutoCompensateRetry(lishiRetryAutoCompensateConfig.getAutoValidityDay(), jdbcTemplate), lishiRetryAutoCompensateConfig.getInitialDelay(), lishiRetryAutoCompensateConfig.getAutoTimes(),
                lishiRetryAutoCompensateConfig.getTimeUnit());
    }


    @Override
    @PreDestroy
    public void destroy() {
        autoCompensatePool.shutdown();
        try {
            if (!autoCompensatePool.awaitTermination(1, TimeUnit.MINUTES)) {
                autoCompensatePool.shutdownNow();
            }
        } catch (InterruptedException e) {
            autoCompensatePool.shutdownNow();
        }
    }

}
