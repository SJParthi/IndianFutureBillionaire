package com.indianfuturebillionaire.kitebot.engine;

import com.indianfuturebillionaire.kitebot.config.AggregatorProperties;
import com.indianfuturebillionaire.kitebot.disruptor.TickEvent;
import com.indianfuturebillionaire.kitebot.risk.RiskManagerService;
import com.lmax.disruptor.dsl.Disruptor;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/***********************************************************************
 * HPC aggregator manager => orchestrates multiple timeframes, meltdown synergy,
 * ring buffer usage, meltdown logs, concurrency stats, feed rate.
 *
 * The DashboardController calls methods like:
 *   getRingBufferUsagePercent()
 *   getRecentMeltdownLogs()
 *   getConcurrencyStatsJson()
 *   getRecentTickRate()
 ***********************************************************************/
@Component
public class MultiTimeframeAggregatorManager {

    private static final Logger logger = LoggerFactory.getLogger(MultiTimeframeAggregatorManager.class);

    private final AggregatorProperties props;
    private final BarConsumer barConsumer;
    private final RiskManagerService riskManager;

    // HPC => assume ring buffer size is 65536
    private static final long RING_BUFFER_SIZE = 65536L;

    // meltdown logs => aggregator meltdown triggers
    private final List<String> meltdownLogs = Collections.synchronizedList(new ArrayList<>());

    // concurrency stats => partial expansions, meltdown triggers
    private final AtomicLong partialExpansions = new AtomicLong(0);
    private final AtomicLong meltdownTriggers  = new AtomicLong(0);

    // feed rate => measure ticks in 1-second window
    private final AtomicLong tickCountWindow = new AtomicLong(0);
    private volatile long lastWindowStartNano = System.nanoTime();
    private volatile long recentTickRate = 0;

    // HPC => store single-timeframe aggregator for each timeframe
    private final Map<String, SingleTimeframeAggregator> subAggregators = new ConcurrentHashMap<>();

    private Disruptor<TickEvent> disruptorRef;

    @Autowired
    @Lazy
    public void setDisruptorRef(Disruptor<TickEvent> disruptor) {
        this.disruptorRef = disruptor;
    }

    public MultiTimeframeAggregatorManager(
            AggregatorProperties p,
            BarConsumer bc,
            RiskManagerService rm
    ) {
        this.props = p;
        this.barConsumer = bc;
        this.riskManager = rm;
    }

    @PostConstruct
    public void init() {
        // HPC => if aggregator props says multiTimeframes: ["1m","5m"], create them
        List<String> tfs = props.getMultiTimeframes();
        for(String tf : tfs) {
            SingleTimeframeAggregator agg = new SingleTimeframeAggregator(tf, props, barConsumer);
            subAggregators.put(tf, agg);
        }
        logger.info("Initialized HPC MultiTimeframeAggregatorManager => tfs={}", tfs);
    }

    /***********************************************************************
     * HPC meltdown aggregator => called from AggregatorEventHandler or
     * wherever. If meltdown is active, we skip aggregator logic.
     * Otherwise, route the tick to each timeframe aggregator.
     * We also measure feed rate => incrementTickRate().
     ***********************************************************************/
    public void processTick(long token, double price, long eventNanoTime, long arrivalNanoTime) {
        if(riskManager.isMeltdownActive()) {
            // meltdown => skip aggregator
            incrementTickRate();
            return;
        }

        // aggregator => route to sub aggregators
        for(SingleTimeframeAggregator agg : subAggregators.values()) {
            agg.onTick(token, price, eventNanoTime, arrivalNanoTime);
        }

        // HPC => if partial expansions happen, we might do partialExpansions.incrementAndGet();
        // meltdown triggers might happen if bar volume is huge => meltdownTriggers.incrementAndGet();

        incrementTickRate();
    }

    private void incrementTickRate() {
        tickCountWindow.incrementAndGet();
        long now = System.nanoTime();
        long diff = now - lastWindowStartNano;
        if(diff >= 1_000_000_000L) {
            long c = tickCountWindow.getAndSet(0);
            recentTickRate = c;
            lastWindowStartNano = now;
        }
    }

    /***********************************************************************
     * HPC meltdown synergy => 1) ring buffer usage percent
     ***********************************************************************/
    public double getRingBufferUsagePercent() {
        if(disruptorRef == null) {
            return 0.0;
        }
        long remain = disruptorRef.getRingBuffer().remainingCapacity();
        long used = RING_BUFFER_SIZE - remain;
        return ((double)used / (double)RING_BUFFER_SIZE) * 100.0;
    }

    /***********************************************************************
     * HPC meltdown synergy => 2) meltdown logs
     ***********************************************************************/
    public List<String> getRecentMeltdownLogs() {
        synchronized(meltdownLogs) {
            return new ArrayList<>(meltdownLogs);
        }
    }

    public void addMeltdownLog(String msg) {
        synchronized(meltdownLogs) {
            meltdownLogs.add(msg);
            if(meltdownLogs.size() > 200) {
                meltdownLogs.remove(0);
            }
        }
    }

    public void incrementMeltdownTriggerCount() {
        meltdownTriggers.incrementAndGet();
    }

    /***********************************************************************
     * HPC meltdown synergy => 3) concurrency stats JSON
     ***********************************************************************/
    public String getConcurrencyStatsJson() {
        long pCount = partialExpansions.get();
        long mCount = meltdownTriggers.get();
        // HPC disclaimers => real code might store timeseries data
        String json = """
        {
           "labels": ["10:00","10:01","10:02","10:03"],
           "partialBarCount": [%d,%d,%d,%d],
           "meltdownTriggers": [%d,%d,%d,%d]
        }
        """.formatted(
                pCount, pCount+1, pCount+2, pCount+3,
                mCount, mCount+1, mCount+2, mCount+3
        );
        return json;
    }

    /***********************************************************************
     * HPC meltdown synergy => 4) feed rate => ticks/sec
     ***********************************************************************/
    public long getRecentTickRate() {
        return recentTickRate;
    }


    /**
     * HPC meltdown synergy => aggregator event handler can check
     * ring buffer usage => we return disruptor's remaining capacity
     */
    public long getRingBufferRemainingCapacity() {
        return disruptorRef.getRingBuffer().remainingCapacity();
    }

    /**
     * HPC meltdown synergy => sidecar might check if usage is low for a period
     * We'll say usage is "low" if feed rate < 1000 for 3 consecutive calls
     */
    private int lowUsageConsecutiveCount = 0;

    public boolean isUsageLowForPeriod() {
        if(getRecentTickRate() < 1000) {
            lowUsageConsecutiveCount++;
        } else {
            lowUsageConsecutiveCount = 0;
        }
        return (lowUsageConsecutiveCount >= 3);
    }
}
