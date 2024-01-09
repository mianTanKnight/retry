create table if not exists `retry_event_compensation`(
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '数据库主键',
    `busKey` VARCHAR(255) NOT NULL COMMENT '业务主键',
    `type` VARCHAR(20) NOT NULL DEFAULT 'auto' COMMENT '处理方式：自动（auto），manual（人工介入）',
    `retryMode` TINYINT NOT NULL COMMENT '数据状态：0:异步 1: 同步',
    `dataStatus` TINYINT NOT NULL COMMENT '数据状态：0 初始化，1：成功，2：失败',
    `className` VARCHAR(200) NOT NULL COMMENT '完整类名',
    `methodName` VARCHAR(100) NOT NULL COMMENT '方法名',
    `reqArgsType` VARCHAR(500) NOT NULL COMMENT '方法入参类型',
    `reqArgs` TEXT NOT NULL COMMENT '方法入参参数',
    `retryStrategy` VARCHAR(100) NOT NULL COMMENT '重试策略',
    `retryCount` INT NOT NULL COMMENT '重试次数',
    `resultMsg`  VARCHAR(550) DEFAULT NULL COMMENT '返回信息/错误信息',
    `autoCompensate` TINYINT DEFAULT 0 COMMENT '自动补偿：0:不开启 1: 开启 ,2：已经自动重试过',
    `retryCreate` TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    `retryModified` TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) COMMENT '更新时间',
    PRIMARY KEY (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='重试事件补偿';