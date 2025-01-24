package com.indianfuturebillionaire.kitebot.disruptor;

import com.indianfuturebillionaire.kitebot.engine.DoubleBufferAggregatorManager;
import com.indianfuturebillionaire.kitebot.risk.RiskManagerService;
import com.lmax.disruptor.EventHandler;
import org.slf4j.*;
import org.springframework.stereotype.Component;

/**
 * HPC meltdown synergy => ring buffer consumer => ephemeral aggregator => meltdown skip => partial meltdown
 */
@Component
public class AggregatorEventHandler implements EventHandler<TickEvent> {

    private static final Logger logger = LoggerFactory.getLogger(AggregatorEventHandler.class);

    private final DoubleBufferAggregatorManager aggregatorManager;
    private final RiskManagerService riskManager;

    public AggregatorEventHandler(DoubleBufferAggregatorManager aggregatorManager,
                                  RiskManagerService riskManager) {
        this.aggregatorManager = aggregatorManager;
        this.riskManager = riskManager;
    }

    @Override
    public void onEvent(TickEvent event, long sequence, boolean endOfBatch) {
        try {
            aggregatorManager.processTick(
                    event.getInstrumentToken(),
                    event.getLastTradedPrice(),
                    event.getEventNanoTime(),
                    event.getArrivalNanoTime()
            );
            // HPC meltdown synergy => ring usage meltdown
            long remain = aggregatorManager.getRingBufferRemainingCapacity();
            double usage = aggregatorManager.getRingBufferUsage(remain);
            if(!riskManager.isMeltdownActive()) {
                double threshold = riskManager.getMeltdownRingBufferUsageThreshold();
                if(usage> threshold) {
                    logger.warn("Ring buffer usage={} > meltdown threshold={}, meltdown triggered!", usage, threshold);
                    riskManager.activateMeltdownMode();
                }
            }
        } catch(Exception e) {
            logger.error("Aggregator error => token={}, price={}, e=", event.getInstrumentToken(), event.getLastTradedPrice(), e);
        } finally {
            event.clear();
        }
    }
}
