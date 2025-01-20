package com.indianfuturebillionaire.kitebot.disruptor;

import com.indianfuturebillionaire.kitebot.engine.MultiTimeframeAggregatorManager;

import com.indianfuturebillionaire.kitebot.risk.RiskManagerService;
import com.lmax.disruptor.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/***********************************************************************
 * HPC aggregator event handler => minimal overhead => meltdown synergy:
 * we pass ticks to aggregator. If meltdown is active, aggregator might skip.
 ***********************************************************************/
@Component
public class AggregatorEventHandler implements EventHandler<TickEvent> {

    private static final Logger logger = LoggerFactory.getLogger(AggregatorEventHandler.class);
    private MultiTimeframeAggregatorManager aggregatorManager;

    @Autowired
    public void setAggregatorManager(@Lazy MultiTimeframeAggregatorManager mgr) {
        this.aggregatorManager = mgr;
    }
    private final RiskManagerService riskManager;
    private final long ringBufferSize;

    public AggregatorEventHandler(RiskManagerService rm) {
        this.riskManager = rm;
        this.ringBufferSize = 65536L; // match RING_BUFFER_SIZE in DisruptorConfig
    }

    @Override
    public void onEvent(TickEvent event, long sequence, boolean endOfBatch) {
        try {
            // HPC meltdown synergy => aggregator is O(1).
            aggregatorManager.processTick(
                    event.getInstrumentToken(),
                    event.getLastTradedPrice(),
                    event.getEventNanoTime(),
                    event.getArrivalNanoTime()
            );

            // Optional HPC meltdown check => ring buffer usage
            long remaining = aggregatorManager.getRingBufferRemainingCapacity();
            double usage = 1.0 - ((double)remaining / ringBufferSize);
            if(!riskManager.isMeltdownActive()) {
                double threshold = riskManager.getMeltdownRingBufferUsageThreshold();
                if(usage > threshold) {
                    logger.warn("Ring buffer usage={} > meltdown threshold={}, meltdown triggered!", usage, threshold);
                    riskManager.activateMeltdownMode();
                }
            }

        } catch(Exception e) {
            logger.error("Aggregator error => token={}, price={}",
                    event.getInstrumentToken(), event.getLastTradedPrice(), e);
        } finally {
            event.clear();
        }
    }
}
