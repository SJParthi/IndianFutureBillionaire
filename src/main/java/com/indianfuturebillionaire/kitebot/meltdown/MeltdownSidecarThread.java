package com.indianfuturebillionaire.kitebot.meltdown;

import com.indianfuturebillionaire.kitebot.engine.MultiTimeframeAggregatorManager;
import com.indianfuturebillionaire.kitebot.risk.RiskManagerService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/***********************************************************************
 * HPC meltdown sidecar => runs asynchronously, checking aggregator feed rate
 * or partial bar expansions => triggers meltdown if we predict overload.
 * aggregator remains minimal => sidecar does meltdown logic out of band.
 ***********************************************************************/
@Component
public class MeltdownSidecarThread implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(MeltdownSidecarThread.class);

    private final RiskManagerService riskManager;
    private final MultiTimeframeAggregatorManager aggregatorManager;
    private volatile boolean running = true;
    private Thread sidecarThread;

    // Example HPC => meltdown if feedRate > meltdownSpikeRate
    // from application.yml => we might get from aggregator props or risk
    private final long meltdownSpikeThresholdRate = 20000L; // ticks/sec example

    public MeltdownSidecarThread(RiskManagerService rm,
                                 MultiTimeframeAggregatorManager mgr) {
        this.riskManager = rm;
        this.aggregatorManager = mgr;
    }

    @PostConstruct
    public void startSidecar() {
        sidecarThread = new Thread(this, "HPCMeltdownSidecar");
        sidecarThread.setDaemon(true); // HPC meltdown synergy => background
        sidecarThread.start();
    }

    @Override
    public void run() {
        // HPC meltdown synergy => loop every 100ms
        while(running) {
            try {
                Thread.sleep(100);

                if(!riskManager.isMeltdownActive()) {
                    // HPC meltdown synergy => check aggregator's feed rate
                    long feedRate = aggregatorManager.getRecentTickRate();
                    if(feedRate > meltdownSpikeThresholdRate) {
                        logger.warn("Sidecar meltdown => feedRate={} > meltdownSpikeThresholdRate={}",
                                feedRate, meltdownSpikeThresholdRate);
                        riskManager.activateMeltdownMode();
                    }
                } else {
                    // meltdown is active => maybe check if we can recover
                    // HPC => if aggregator usage is normal for ~some time
                    if(aggregatorManager.isUsageLowForPeriod()) {
                        logger.info("Sidecar meltdown => usage low => deactivate meltdown");
                        riskManager.deactivateMeltdown();
                    }
                }

            } catch (InterruptedException e) {
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
        if(sidecarThread != null) {
            sidecarThread.interrupt();
        }
    }
}
