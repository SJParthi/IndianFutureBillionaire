package com.indianfuturebillionaire.kitebot.meltdown;

import com.indianfuturebillionaire.kitebot.engine.DoubleBufferAggregatorManager;
import com.indianfuturebillionaire.kitebot.risk.RiskManagerService;
import jakarta.annotation.PostConstruct;
import org.slf4j.*;
import org.springframework.stereotype.Component;

/***********************************************************************
 * HPC meltdown synergy => sidecar => checks feed rate => meltdown if above threshold => aggregator skip
 */
@Component
public class MeltdownSidecarThread implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(MeltdownSidecarThread.class);

    private final RiskManagerService riskManager;
    private final DoubleBufferAggregatorManager aggregatorManager;

    private volatile boolean running = true;
    private Thread sidecarThread;

    // HPC meltdown synergy => feedRate meltdown if > meltdownSpikeThresholdRate
    private long meltdownSpikeThresholdRate = 20000L; // e.g. from config or reference

    public MeltdownSidecarThread(RiskManagerService rm,
                                 DoubleBufferAggregatorManager mgr) {
        this.riskManager = rm;
        this.aggregatorManager = mgr;
    }

    @PostConstruct
    public void startSidecar() {
        sidecarThread = new Thread(this, "HPCMeltdownSidecar");
        sidecarThread.setDaemon(true);
        sidecarThread.start();
    }

    @Override
    public void run() {
        while(running) {
            try {
                Thread.sleep(100);
                if(!riskManager.isMeltdownActive()) {
                    long feedRate = aggregatorManager.getRecentTickRate();
                    if(feedRate > meltdownSpikeThresholdRate) {
                        logger.warn("Sidecar meltdown => feedRate={} > meltdownSpikeThresholdRate={}",
                                feedRate, meltdownSpikeThresholdRate);
                        riskManager.activateMeltdownMode();
                    }
                } else {
                    if(aggregatorManager.isUsageLowForPeriod()) {
                        logger.info("Sidecar meltdown => usage low => deactivate meltdown");
                        riskManager.deactivateMeltdown();
                    }
                }
            } catch(InterruptedException e) {
                logger.warn("Meltdown sidecar interrupted => stopping thread");
                Thread.currentThread().interrupt();
                return;
            } catch(Exception e) {
                logger.error("Sidecar meltdown error => ", e);
            }
        }
    }

    public void stopSidecar() {
        running = false;
        if(sidecarThread!=null) sidecarThread.interrupt();
    }
}
