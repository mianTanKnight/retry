package com.lishicloud.lretry.strategy;




import com.lishicloud.lretry.config.LishiRetryConfig;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 全抖动重试
 *
 * @author ztq
 */
public class FullJitterRetry extends AbsRetryConfigStrategy {

    private final ThreadLocalRandom threadLocalRandom;
    private final int minRadon;
    private final int maxRadon;

    public FullJitterRetry(LishiRetryConfig config) {
        super(config);
        threadLocalRandom = ThreadLocalRandom.current();
        minRadon = Integer.parseInt(config.getRadiusTime().split("-")[0]);
        maxRadon = Integer.parseInt(config.getRadiusTime().split("-")[1]);
    }

    @Override
    public long nextDuration() {
        return threadLocalRandom.nextInt(minRadon, maxRadon);
    }
}
