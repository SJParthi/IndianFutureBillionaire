package com.indianfuturebillionaire.kitebot.risk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/***********************************************************************
 * HPC meltdown manager => daily limit, meltdown triggers from bar volume,
 * ring buffer usage, or sidecar meltdown approach.
 * We store meltdownActive in an AtomicBoolean => lock-free meltdown toggles.
 ***********************************************************************/
@Service
public class RiskManagerService {

    private static final Logger logger = LoggerFactory.getLogger(RiskManagerService.class);

    private final int meltdownThreshold;
    private final double meltdownRingBufferUsageThreshold;
    private final int dailyOrderLimit;

    private int orderCount = 0;
    private final AtomicBoolean meltdownActive = new AtomicBoolean(false);

    public RiskManagerService(
            @Value("${risk.meltdown-threshold}") int meltdownThreshold,
            @Value("${risk.meltdown-ring-buffer-usage-threshold}") double meltdownRingBufUsage,
            @Value("${risk.daily-limit}") int dailyLimit
    ) {
        this.meltdownThreshold = meltdownThreshold;
        this.meltdownRingBufferUsageThreshold = meltdownRingBufUsage;
        this.dailyOrderLimit = dailyLimit;
    }

    public boolean isOrderAllowed(String side, long token, int qty) {
        if(meltdownActive.get()) {
            logger.warn("Meltdown => blocking new orders => side={}, token={}", side, token);
            return false;
        }
        if(orderCount >= dailyOrderLimit) {
            logger.warn("Daily limit => blocking order => side={}, token={}", side, token);
            return false;
        }
        orderCount++;
        return true;
    }

    public void resetDailyCount() {
        orderCount = 0;
        meltdownActive.set(false);
        logger.info("Daily counters reset => meltdownActive={}", meltdownActive.get());
    }

    public void activateMeltdownMode() {
        if(meltdownActive.compareAndSet(false, true)) {
            logger.error("!!! HPC MELTDOWN MODE ACTIVATED => aggregator skip logic engaged !!!");
        }
    }

    public void deactivateMeltdown() {
        if(meltdownActive.compareAndSet(true, false)) {
            logger.info("Meltdown deactivated => aggregator resumed normal HPC updates");
        }
    }

    public boolean isMeltdownActive() {
        return meltdownActive.get();
    }

    public int getMeltdownThreshold() { return meltdownThreshold; }

    public double getMeltdownRingBufferUsageThreshold() {
        return meltdownRingBufferUsageThreshold;
    }
}
