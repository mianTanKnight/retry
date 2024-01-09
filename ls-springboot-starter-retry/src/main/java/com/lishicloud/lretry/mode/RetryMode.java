package com.lishicloud.lretry.mode;

/**
 * 通过重试模式和异常类型确定重试策略
 * 例如 timeout 就可以绑定 固定间隔重试和指数间隔重试 前提是异步模型
 * 同步模型 只有立即重试
 * @author ztq
 */
public interface RetryMode {
}
