package com.indianfuturebillionaire.kitebot.engine;

import com.indianfuturebillionaire.kitebot.config.AggregatorProperties;
import com.indianfuturebillionaire.kitebot.model.Bar;
import org.slf4j.*;
import java.util.HashMap;
import java.util.Map;

/**
 * HPC meltdown synergy => aggregator for a single timeframe => partial bars, quiet zones, sub-cycle expansions
 * If meltdown skip is triggered, aggregator is not called => ensures sub-10ms usage under overload
 */
public class SingleTimeframeAggregator {
    private static final Logger logger = LoggerFactory.getLogger(SingleTimeframeAggregator.class);

    // HPC meltdown synergy => which timeframe => e.g. "1m"
    private final String timeframe;
    private final AggregatorProperties props;
    private final BarConsumer barConsumer;

    // HPC meltdown synergy => track aggregator state per instrument => O(N)
    private final Map<Long, BarState> barStates = new HashMap<>();

    public SingleTimeframeAggregator(String timeframe, AggregatorProperties props, BarConsumer barConsumer) {
        this.timeframe = timeframe;
        this.props = props;
        this.barConsumer = barConsumer;
    }

    /**
     * HPC meltdown synergy => onTick => aggregator logic => sub-10ms usage if meltdown not active
     */
    public void onTick(long token, double price, long evtTime, long arrivalTime) {
        // HPC meltdown synergy => map lookup for aggregator state => O(1) average
        BarState st = barStates.computeIfAbsent(token, t->new BarState(t, timeframe));

        // HPC meltdown synergy => if quiet zone => finalize bar
        checkQuietZone(st, evtTime);

        // HPC meltdown synergy => sub-cycle expansions if enabled
        if(props.isSubCycle() && st.barOpen) {
            maybeSplitBarMidCycle(st, price, evtTime);
        }

        // HPC meltdown synergy => if bar not open => start new bar
        if(!st.barOpen) {
            startBar(st, price, evtTime);
        } else {
            // HPC meltdown synergy => update existing bar
            updateBar(st, price, evtTime);
        }

        st.lastTickTime = evtTime;
    }

    /**
     * HPC meltdown synergy => if last tick was too long => finalize bar => quiet zone
     */
    private void checkQuietZone(BarState st, long evtTime) {
        if(st.barOpen) {
            long diffSec = (evtTime - st.lastTickTime) / 1_000_000_000L;
            if(diffSec > props.getQuietZoneThresholdMinutes() * 60) {
                logger.debug("quietZone => finalize => token={}, tf={}", st.token, timeframe);
                finalizeBar(st, "QUIET_ZONE");
            }
        }
    }

    /**
     * HPC meltdown synergy => sub-cycle expansions => forcibly finalize bar mid-cycle => half timeframe
     */
    private void maybeSplitBarMidCycle(BarState st, double price, long evtTime) {
        long barDur = parseTimeframeNanos(timeframe);
        long halfDur = barDur / 2;
        long elapsed = evtTime - st.openTime;
        if(elapsed > halfDur && !st.subCycleSplitDone) {
            logger.debug("[sub-cycle] => forcibly finalize => token={}, tf={}", st.token, timeframe);
            finalizeBar(st, "SUBCYCLE_SPLIT");
            startBar(st, price, evtTime);
            st.subCycleSplitDone = true;
        }
    }

    /**
     * HPC meltdown synergy => start new bar => minimal object overhead => sub-10ms aggregator
     */
    private void startBar(BarState st, double price, long evtTime) {
        st.barOpen = true;
        st.openTime = evtTime;
        st.open = price;
        st.high = price;
        st.low  = price;
        st.close= price;
        st.volume= 1;
        logger.trace("startBar => token={}, tf={}, open={}", st.token, timeframe, price);
    }

    /**
     * HPC meltdown synergy => update bar => partial bars => meltdown synergy =>  sub-10ms aggregator
     */
    private void updateBar(BarState st, double price, long evtTime) {
        if(price > st.high) st.high = price;
        if(price < st.low ) st.low  = price;
        st.close = price;
        st.volume++;

        // HPC meltdown synergy => partial bar if price moves >= partialBarThreshold
        if(props.isEnablePartialBars()) {
            double perc = Math.abs(price - st.open)/st.open;
            if(perc >= props.getPartialBarThresholdPercent()) {
                logger.debug("Shock bar => finalize => token={}, tf={}, perc={}", st.token, timeframe, perc);
                finalizeBar(st, "SHOCK");
                return;
            }
        }

        // HPC meltdown synergy => if bar duration exceeded => do "soft close" => finalize
        long barDur = parseTimeframeNanos(timeframe);
        long elapsed = evtTime - st.openTime;
        if(elapsed >= barDur) {
            if(!st.softClosed) {
                st.softClosed = true;
            } else {
                finalizeBar(st, "NORMAL");
            }
        }
    }

    /**
     * HPC meltdown synergy => finalize bar => calls barConsumer => meltdown synergy => might trigger meltdown on volume
     */
    private void finalizeBar(BarState st, String reason) {
        if(!st.barOpen) return;
        Bar bar = new Bar();
        bar.setInstrumentToken(st.token);
        bar.setCompanyName(st.companyName);
        bar.setTimeframe(timeframe);
        bar.setOpen(st.open);
        bar.setHigh(st.high);
        bar.setLow(st.low);
        bar.setClose(st.close);
        bar.setVolume(st.volume);
        bar.setStartTime(st.openTime);
        bar.setEndTime(st.lastTickTime);
        bar.setReason(reason);

        // HPC meltdown synergy => pass final bar to consumer => can meltdown if volume too big
        barConsumer.onBarFinalized(bar);

        st.barOpen = false;
        st.softClosed = false;
        st.subCycleSplitDone = false;
        logger.trace("finalizeBar => token={}, tf={}, reason={}, O/H/L/C={}/{}/{}/{}, vol={}",
                st.token, st.companyName, timeframe, reason, st.open, st.high, st.low, st.close, st.volume);
    }

    /**
     * HPC meltdown synergy => parse timeframe => "1m" => 60_000_000_000 nanos
     */
    private long parseTimeframeNanos(String tf) {
        if(tf.endsWith("m")) {
            long mins = Long.parseLong(tf.substring(0, tf.length()-1));
            return mins * 60_000_000_000L;
        }
        // HPC meltdown synergy => default => 1m
        return 60_000_000_000L;
    }

    /**
     * HPC meltdown synergy => aggregator state per instrument => sub-10ms aggregator if meltdown skip
     */
    private static class BarState {
        long token;
        String companyName;
        boolean barOpen;
        boolean softClosed;
        boolean subCycleSplitDone;
        long openTime;
        long lastTickTime;
        double open, high, low, close;
        long volume;
        String timeframe;

        BarState(long token, String timeframe) {
            this.token = token;
            this.timeframe = timeframe;
        }
    }
}
