package com.indianfuturebillionaire.kitebot.engine;

import com.indianfuturebillionaire.kitebot.config.AggregatorProperties;
import com.indianfuturebillionaire.kitebot.disruptor.TickEvent;
import com.indianfuturebillionaire.kitebot.risk.RiskManagerService;
import com.lmax.disruptor.dsl.Disruptor;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * HPC meltdown synergy => aggregator manager using double-buffer ephemeral aggregator approach.
 * If meltdown => skip aggregator. If partial meltdown => skip some instruments or partial logic.
 */
@Component
public class DoubleBufferAggregatorManager {

    private static final Logger logger = LoggerFactory.getLogger(DoubleBufferAggregatorManager.class);

    private final AggregatorProperties props;
    private final BarConsumer barConsumer;
    private final RiskManagerService riskManager;

    private Disruptor<TickEvent> disruptorRef;

    // meltdown logs => HPC meltdown synergy => displayed in dashboard
    private final List<String> meltdownLogs = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong meltdownTriggers = new AtomicLong(0);

    // feed rate => HPC meltdown synergy => aggregator usage => meltdown sidecar
    private final AtomicLong tickCountWindow = new AtomicLong(0);
    private volatile long lastWindowStartNano = System.nanoTime();
    private volatile long recentTickRate = 0;

    // HPC meltdown synergy => ephemeral aggregator => store DoubleBufferBarState per instrument
    private final Map<Long, DoubleBufferBarState> instrumentAggregators = new ConcurrentHashMap<>();

    // HPC meltdown synergy => partial meltdown usage threshold => skip partial aggregator if usage is above 80% but below meltdown threshold
    private final double partialMeltdownUsageThreshold = 0.80;

    public DoubleBufferAggregatorManager(AggregatorProperties props,
                                         BarConsumer barConsumer,
                                         RiskManagerService riskManager) {
        this.props = props;
        this.barConsumer = barConsumer;
        this.riskManager = riskManager;
    }

    @Lazy
    public void setDisruptorRef(Disruptor<TickEvent> disruptorRef) {
        this.disruptorRef = disruptorRef;
    }

    @PostConstruct
    public void init() {
        logger.info("DoubleBufferAggregatorManager => ephemeral aggregator approach => HPC meltdown synergy");
    }

    /**
     * HPC meltdown synergy => called by AggregatorEventHandler => meltdown skip if meltdown
     */
    public void processTick(long token, double price, long evtNano, long arrivalNano) {
        incrementTickRate();

        if(riskManager.isMeltdownActive()) {
            // HPC meltdown => aggregator skip => O(1)
            return;
        }

        // HPC meltdown => partial meltdown if ring usage in [80%, meltdownThreshold)
        double usage = getRingBufferUsage(getRingBufferRemainingCapacity());
        double meltdownUsageThreshold = riskManager.getMeltdownRingBufferUsageThreshold();
        if(usage > partialMeltdownUsageThreshold && usage < meltdownUsageThreshold) {
            // HPC meltdown => partial skip => e.g. skip aggregator for half instruments
            if(token % 2 == 0) {
                // HPC meltdown synergy => skip aggregator for some tokens
                return;
            }
        }

        // HPC meltdown synergy => ephemeral aggregator => handleTick
        DoubleBufferBarState st = instrumentAggregators.computeIfAbsent(token,
                t-> new DoubleBufferBarState(t, props.getDefaultTimeframe()));
        st.handleTick(price, evtNano, props, barConsumer);
    }

    private void incrementTickRate() {
        tickCountWindow.incrementAndGet();
        long now = System.nanoTime();
        if(now - lastWindowStartNano >= 1_000_000_000L) {
            long c = tickCountWindow.getAndSet(0);
            recentTickRate = c;
            lastWindowStartNano = now;
        }
    }

    public long getRingBufferRemainingCapacity() {
        if(disruptorRef==null) {
            return props.getRingBufferSize();
        }
        return disruptorRef.getRingBuffer().remainingCapacity();
    }

    public double getRingBufferUsage(long remain) {
        long total = props.getRingBufferSize();
        long used = total - remain;
        return (used*1.0)/ total;
    }

    public double getRingBufferUsagePercent() {
        long remain = getRingBufferRemainingCapacity();
        return getRingBufferUsage(remain)*100.0;
    }

    public long getRecentTickRate() {
        return recentTickRate;
    }

    // HPC meltdown synergy => meltdown logs => if you want to record meltdown triggers, etc.
    public void addMeltdownLog(String msg) {
        synchronized(meltdownLogs) {
            meltdownLogs.add(msg);
            if(meltdownLogs.size()>200) meltdownLogs.remove(0);
        }
    }

    public List<String> getRecentMeltdownLogs() {
        synchronized(meltdownLogs) {
            return new ArrayList<>(meltdownLogs);
        }
    }

    public boolean isUsageLowForPeriod() {
        // HPC meltdown synergy => if feed rate < 1000 => meltdown can end
        return (getRecentTickRate()<1000);
    }

    public String getConcurrencyStatsJson() {
        long m = meltdownTriggers.get();
        return """
        {
            "meltdownTriggers": %d
        }
        """.formatted(m);
    }
}
