package com.lishicloud.lretry.manager;

import com.lishicloud.lretry.config.LishAutoDbConfiguration;
import com.lishicloud.lretry.config.LishiRetryAutoCompensateConfig;
import com.lishicloud.lretry.constant.RetryConstant;
import com.lishicloud.lretry.exception.RetryFailInfo;
import com.lishicloud.lretry.strategy.AbsRetryConfigStrategy;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.LineNumberReader;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * 重试事件的落库简单实现
 *
 * @author ztq
 */
@Slf4j
@Component
@Scope("singleton")
@ConditionalOnBean(LishAutoDbConfiguration.class)
public class LishiRetryJdbcManager implements IManager {

    private final JdbcTemplate jdbcTemplate;
    private final PlatformTransactionManager transactionManager;


    @Autowired
    public LishiRetryJdbcManager(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionManager = transactionManager;
    }

    /**
     * 注意  syncRegisterInitialRetryData 和 syncRegisterEndRetryData
     * 可能会在不同的线程中执行的 所以需要手动事务提交
     * 但它们一定会有先后顺序 syncRegisterInitialRetryData会先执行 这个在AsyncMode中得到保证
     */
    public void syncRegisterInitialRetryData(AbsRetryConfigStrategy strategy, LishiRetryManager.RetryEntity entity) {
        buildAndExecuteInsertSql(strategy, entity);
    }

    public void syncRegisterEndRetryData(String key, Object result) {
        buildAndExecuteUpdateSql(key, result);
    }

    @PostConstruct
    @Override
    public void init() throws Exception {
        //如果已经存在 不需要再浪费IO读取
        if (isTableExists(RetryConstant.TABLE_NAME)) {
            log.info("[{}] table Already exists give up init", RetryConstant.TABLE_NAME);
            return;
        }
        for (String ignored : getFileSqlStr().split(";")) {
            if (StringUtils.isNotBlank(ignored)) {
                jdbcTemplate.execute(ignored);
            }
        }
        log.info("PostConstruct InitTable Successful!");
    }

    public void buildAndExecuteUpdateSql(String key, Object result) {
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            String sql = "UPDATE retry_event_compensation SET dataStatus=?,resultMsg = ?,retryModified =? WHERE busKey=?";
            String resultStr = result.toString();
            resultStr = resultStr.length() > RetryConstant.MAX_MESSAGE_LENGTH ? resultStr.substring(0, RetryConstant.MAX_MESSAGE_LENGTH) : resultStr;
            jdbcTemplate.update(sql, result instanceof RetryFailInfo ? RetryConstant.RETRY_FAIL : RetryConstant.RETRY_SUCCESSFUL, resultStr, new Date(), key);
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
            e.printStackTrace();
            throw e;
        }
        log.info("Data BusKey:[{}] is successfully Update", key);
    }

    public void buildAndExecuteInsertSql(AbsRetryConfigStrategy strategy, LishiRetryManager.RetryEntity entity) {
        String checkSql = "SELECT COUNT(*) FROM retry_event_compensation WHERE busKey = ?";
        int count = jdbcTemplate.queryForObject(checkSql, new Object[]{entity.getKey()}, Integer.class);
        if (count > 0) {
            log.info("Data BusKey:[{}] is Already Exists give up Insert", entity.getKey());
            return;
        }
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            String sql = "INSERT INTO retry_event_compensation (id,busKey,type,retryMode,dataStatus,className,methodName" +
                    ",reqArgsType,reqArgs,retryStrategy,retryCount,resultMsg,autoCompensate,retryCreate,retryModified) VALUES (?,?" +
                    ",?,?,?,?,?,?,?,?,?,?,?,?,?)";
            ReTryInfoEntity auto = ReTryInfoEntity.builder().
                    busKey(entity.getKey()).
                    type(RetryConstant.RETRY_TYPE_AUTO).
                    retryMode(entity.getRetryMode()).
                    dataStatus(RetryConstant.RETRY_INIT).
                    className(entity.getClassName()).
                    methodName(entity.getMethodName()).
                    reqArgs(entity.getJsonArgs()).
                    reqArgsType(entity.getArgs()[0].getClass().getName()).
                    retryStrategy(strategy.getClass().getName()).
                    retryCount(strategy.getInitialTimes()).
                    retryCreate(new Date()).
                    autoCompensate(RetryConstant.AUTO_COMPENSATE_DISABLE).build();
            this.jdbcTemplate.update(sql, auto.toObjs());
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
            e.printStackTrace();
            throw e;
        }
        log.info("Data BusKey:[{}] is successfully written to the database", entity.getKey());
    }

    private String getFileSqlStr() throws IOException {
        Resource classPathResource = new ClassPathResource(RetryConstant.INIT_SQL);
        EncodedResource encodedResource = new EncodedResource(classPathResource, "utf-8");
        try (LineNumberReader lnr = new LineNumberReader(encodedResource.getReader())) {
            return ScriptUtils.readScript(lnr, "--", ";", "*/");
        }
    }

    public boolean isTableExists(String tableName) {
        try {
            jdbcTemplate.execute("SELECT 1 FROM " + tableName + " WHERE 1 = 0");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void destroy() {
        // empty impl
    }

    @Data
    @Builder
    public static class ReTryInfoEntity {
        private Integer id;
        private String busKey;
        private String type;
        private Byte retryMode;
        //数据状态：0 初始化，1：成功，2：失败
        private Byte dataStatus;
        /*** 完整类名*/
        private String className;
        /*** 方法名*/
        private String methodName;
        /*** 方法入参类型*/
        private String reqArgsType;
        /*** 方法入参参数*/
        private String reqArgs;
        private String retryStrategy;
        /*** 重试次数*/
        private Integer retryCount;
        /*** 错误描述*/
        private String resultMsg;
        /***是否开启自动补偿*/
        private Byte autoCompensate;
        /*** 创建时间*/
        private Date retryCreate;
        /*** 修改时间*/
        private Date retryModified;

        public Object[] toObjs() {
            Field[] fields = this.getClass().getDeclaredFields();
            Object[] objs = new Object[fields.length];
            try {
                for (int i = 0; i < fields.length; i++) {
                    fields[i].setAccessible(true);
                    objs[i] = fields[i].get(this);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return objs;
        }
    }

    public static class ReTryInfoEntityMapper implements RowMapper<ReTryInfoEntity> {
        @Override
        public ReTryInfoEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            return ReTryInfoEntity.builder()
                    .id(rs.getInt("id"))
                    .busKey(rs.getString("busKey"))
                    .type(rs.getString("type"))
                    .retryMode(rs.getByte("retryMode"))
                    .dataStatus(rs.getByte("dataStatus"))
                    .className(rs.getString("className"))
                    .methodName(rs.getString("methodName"))
                    .reqArgsType(rs.getString("reqArgsType"))
                    .reqArgs(rs.getString("reqArgs"))
                    .retryStrategy(rs.getString("retryStrategy"))
                    .retryCount(rs.getInt("retryCount"))
                    .resultMsg(rs.getString("resultMsg"))
                    .autoCompensate(rs.getByte("autoCompensate"))
                    .retryCreate(rs.getTimestamp("retryCreate"))
                    .retryModified(rs.getTimestamp("retryModified"))
                    .build();
        }
    }


}
