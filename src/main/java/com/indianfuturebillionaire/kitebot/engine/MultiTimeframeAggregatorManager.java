package com.indianfuturebillionaire.kitebot.engine;

import com.indianfuturebillionaire.kitebot.config.AggregatorProperties;
import com.indianfuturebillionaire.kitebot.disruptor.TickEvent;
import com.indianfuturebillionaire.kitebot.risk.RiskManagerService;
import com.lmax.disruptor.dsl.Disruptor;
import jakarta.annotation.PostConstruct;
import org.slf4j.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * HPC meltdown synergy => manages aggregator for multiple timeframes => meltdown skip logic
 * Also supports partial meltdown if usage is high but not catastrophic => skip some instruments only
 */
@Component
public class MultiTimeframeAggregatorManager {
    private static final Logger logger = LoggerFactory.getLogger(MultiTimeframeAggregatorManager.class);

    private final AggregatorProperties props;
    private final BarConsumer barConsumer;
    private final RiskManagerService riskManager;

    private Disruptor<TickEvent> disruptorRef;

    // meltdown logs => shown in dashboard => HPC meltdown synergy
    private final List<String> meltdownLogs = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong partialExpansions = new AtomicLong(0);
    private final AtomicLong meltdownTriggers  = new AtomicLong(0);

    // HPC meltdown synergy => track feed rate => meltdown sidecar uses it
    private final AtomicLong tickCountWindow = new AtomicLong(0);
    private volatile long lastWindowStartNano = System.nanoTime();
    private volatile long recentTickRate = 0;

    // HPC meltdown synergy => aggregator per timeframe => O(M) timeframes
    private final Map<String, SingleTimeframeAggregator> subAggregators = new ConcurrentHashMap<>();

    // HPC meltdown synergy => optional partial meltdown threshold => skip only partial aggregator
    // This is an advanced approach for sub-10ms usage => skipping only some instruments
    private final double partialMeltdownUsageThreshold = 0.80; // usage 80% => partial meltdown

    public MultiTimeframeAggregatorManager(AggregatorProperties props,
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
        // HPC meltdown synergy => create aggregator for each timeframe from config
        List<String> tfs = props.getMultiTimeframes();
        for(String tf : tfs) {
            SingleTimeframeAggregator agg = new SingleTimeframeAggregator(tf, props, barConsumer);
            subAggregators.put(tf, agg);
        }
        logger.info("Initialized HPC MultiTimeframeAggregatorManager => tfs={}", tfs);
    }

    /**
     * HPC meltdown synergy => processTick => meltdown skip logic => partial meltdown approach
     */
    public void processTick(long token, double price, long eventNano, long arrivalNano) {
        // HPC meltdown synergy => measure feed rate even if meltdown skip
        incrementTickRate();

        // If meltdown fully active => skip aggregator entirely => O(1)
        if(riskManager.isMeltdownActive()) {
            return;
        }

        // HPC meltdown synergy => partial meltdown if usage is high but not above meltdown threshold
        double usage = getRingBufferUsage(getRingBufferRemainingCapacity());
        double meltdownUsageThreshold = riskManager.getMeltdownRingBufferUsageThreshold();

        // If usage > partialMeltdownUsageThreshold but < meltdownUsageThreshold => partial meltdown skip
        if(usage > partialMeltdownUsageThreshold && usage < meltdownUsageThreshold) {
            // HPC meltdown synergy => skip aggregator for half of the timeframes
            // or skip aggregator for certain instruments => demonstration
            partialSkipAggregator(token, price, eventNano, arrivalNano);
            return;
        }

        // HPC meltdown synergy => normal aggregator logic => O(M) => M timeframes
        for(SingleTimeframeAggregator agg : subAggregators.values()) {
            agg.onTick(token, price, eventNano, arrivalNano);
        }
    }

    /**
     * HPC meltdown synergy => partial skip => skip aggregator for half timeframes => demonstration approach
     */
    private void partialSkipAggregator(long token, double price, long evtNano, long arrivalNano) {
        int count = 0;
        for(SingleTimeframeAggregator agg : subAggregators.values()) {
            count++;
            // HPC meltdown synergy => only process aggregator for half the timeframes => others skip
            if(count % 2 == 0) {
                agg.onTick(token, price, evtNano, arrivalNano);
            } else {
                // HPC meltdown synergy => skip aggregator => partial meltdown synergy
            }
        }
    }

    /**
     * HPC meltdown synergy => feed rate => aggregator side => meltdown sidecar uses getRecentTickRate()
     */
    private void incrementTickRate() {
        tickCountWindow.incrementAndGet();
        long now = System.nanoTime();
        long diff = now - lastWindowStartNano;
        // HPC meltdown synergy => every 1 second => measure feed rate
        if(diff >= 1_000_000_000L) {
            long c = tickCountWindow.getAndSet(0);
            recentTickRate = c;
            lastWindowStartNano = now;
        }
    }

    /**
     * HPC meltdown synergy => ring buffer usage => aggregator manager => meltdown triggers
     */
    public long getRingBufferRemainingCapacity() {
        if(disruptorRef == null) {
            // HPC meltdown synergy => fallback if disruptor not set => returns ringBufferSize
            return props.getRingBufferSize();
        }
        return disruptorRef.getRingBuffer().remainingCapacity();
    }

    public double getRingBufferUsage(long remaining) {
        long total = props.getRingBufferSize();
        long used = total - remaining;
        return (used * 1.0) / total;
    }

    public double getRingBufferUsagePercent() {
        long remain = getRingBufferRemainingCapacity();
        return getRingBufferUsage(remain) * 100.0;
    }

    public long getRecentTickRate() {
        return recentTickRate;
    }

    // HPC meltdown synergy => meltdown logs => displayed in dashboard
    public void addMeltdownLog(String msg) {
        synchronized(meltdownLogs) {
            meltdownLogs.add(msg);
            if(meltdownLogs.size() > 200) meltdownLogs.remove(0);
        }
    }
    public List<String> getRecentMeltdownLogs() {
        synchronized(meltdownLogs) {
            return new ArrayList<>(meltdownLogs);
        }
    }

    /**
     * HPC meltdown synergy => concurrency stats => partial expansions or meltdown triggers => for mesmerizing dashboard
     */
    public String getConcurrencyStatsJson() {
        long p = partialExpansions.get();
        long m = meltdownTriggers.get();
        // HPC meltdown synergy => could feed chart.js
        return """
        {
          "partialExpansions": %d,
          "meltdownTriggers": %d
        }
        """.formatted(p, m);
    }

    /**
     * HPC meltdown synergy => sidecar meltdown => if feed rate < 1000 => usage is low => meltdown can end
     */
    public boolean isUsageLowForPeriod() {
        return (getRecentTickRate() < 1000);
    }
}
