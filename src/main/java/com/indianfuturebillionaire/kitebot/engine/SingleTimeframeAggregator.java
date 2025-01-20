package com.indianfuturebillionaire.kitebot.engine;

import com.indianfuturebillionaire.kitebot.config.AggregatorProperties;
import com.indianfuturebillionaire.kitebot.model.Bar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**********************************************************************
 * HPC aggregator for a single timeframe: e.g. "1m".
 * We do partial bar shock logic, quiet zone merges, sub-cycle expansions,
 * meltdown checks if needed. Then finalize bars => calls barConsumer.
 **********************************************************************/
public class SingleTimeframeAggregator {

    private static final Logger logger = LoggerFactory.getLogger(SingleTimeframeAggregator.class);

    private final String timeframe;
    private final AggregatorProperties props;
    private final BarConsumer barConsumer;

    // track aggregator state per instrument
    private final Map<Long, BarState> barStates = new HashMap<>();

    public SingleTimeframeAggregator(String tf, AggregatorProperties p, BarConsumer bc) {
        this.timeframe = tf;
        this.props = p;
        this.barConsumer = bc;
    }

    /**
     * HPC => onTick is O(1). We do minimal object creation.
     */
    public void onTick(long token, double price, long evtTime, long arrivalTime) {
        BarState st = barStates.computeIfAbsent(token, t->new BarState(t, timeframe));

        // check quiet zone => if last tick was too long ago, finalize bar
        checkQuietZone(st, evtTime);

        // sub-cycle expansions => if aggregator.sub-cycle=true, do partial merges
        if(props.isSubCycle() && st.barOpen) {
            maybeSplitBarMidCycle(st, price, evtTime);
        }

        if(!st.barOpen) {
            startBar(st, price, evtTime);
        } else {
            updateBar(st, price, evtTime);
        }
        st.lastTickTime = evtTime;
    }

    private void maybeSplitBarMidCycle(BarState st, double price, long evtTime) {
        // HPC partial example => if half the timeframe has passed & subCycle not done
        long barDur = parseTimeframeNanos(timeframe);
        long halfDur = barDur / 2;
        long elapsed = evtTime - st.openTime;
        if(elapsed > halfDur && !st.subCycleSplitDone) {
            logger.debug("[sub-cycle] => forcibly finalize bar => token={}, tf={}", st.token, timeframe);
            finalizeBar(st, "SUBCYCLE_SPLIT");
            // start new bar with this tick
            startBar(st, price, evtTime);
            st.subCycleSplitDone = true;
        }
    }

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

    private void updateBar(BarState st, double price, long evtTime) {
        if(price > st.high) st.high = price;
        if(price < st.low ) st.low  = price;
        st.close = price;
        st.volume++;

        // partial bar shock => if price moves more than partialBarThreshold
        if(props.isEnablePartialBars()) {
            double perc = Math.abs(price - st.open)/st.open;
            if(perc >= props.getPartialBarThresholdPercent()) {
                logger.debug("Shock bar => finalize => token={}, tf={}, perc={}", st.token, timeframe, perc);
                finalizeBar(st, "SHOCK");
                return;
            }
        }

        // HPC => sub-cycle expansions or meltdown checks if desired
        // locked extremes => not shown here, but can be integrated

        // bar duration check => if we exceed timeframe
        long barDur = parseTimeframeNanos(timeframe);
        long elapsed = evtTime - st.openTime;
        if(elapsed >= barDur) {
            // we do a 'soft close' approach => if we see next tick, finalize
            if(!st.softClosed) {
                st.softClosed = true;
                logger.trace("softClose => token={}, tf={}", st.token, timeframe);
            } else {
                finalizeBar(st, "NORMAL");
            }
        }
    }

    private void finalizeBar(BarState st, String reason) {
        if(!st.barOpen) return;
        if(st.volume < props.getBarMergeThresholdVolume()) {
            logger.debug("Low volume => might merge => token={}, tf={}, vol={}", st.token, timeframe, st.volume);
        }

        // HPC => build final bar => no large object creation beyond Bar
        Bar bar = new Bar();
        bar.setInstrumentToken(st.token);
        bar.setTimeframe(timeframe);
        bar.setOpen(st.open);
        bar.setHigh(st.high);
        bar.setLow(st.low);
        bar.setClose(st.close);
        bar.setVolume(st.volume);
        bar.setStartTime(st.openTime);
        bar.setEndTime(st.openTime + parseTimeframeNanos(timeframe));
        bar.setReason(reason);

        // push to consumer => HPC meltdown logic or strategy
        barConsumer.onBarFinalized(bar);

        // reset bar state
        st.barOpen = false;
        st.softClosed = false;
        st.subCycleSplitDone = false;
        logger.trace("finalizeBar => token={}, tf={}, reason={}, O/H/L/C={}/{}/{}/{}, vol={}",
                st.token, timeframe, reason, st.open, st.high, st.low, st.close, st.volume);
    }

    private void checkQuietZone(BarState st, long evtTime) {
        if(st.barOpen) {
            long diffSec = (evtTime - st.lastTickTime)/1_000_000_000L;
            if(diffSec > props.getQuietZoneThresholdMinutes()*60) {
                logger.debug("quietZone => finalize => token={}, tf={}", st.token, timeframe);
                finalizeBar(st, "QUIET_ZONE");
            }
        }
    }

    private long parseTimeframeNanos(String tf) {
        // e.g. "1m" => 60_000_000_000
        if(tf.endsWith("m")) {
            long minutes = Long.parseLong(tf.substring(0, tf.length()-1));
            return minutes * 60_000_000_000L;
        }
        // default => 1m
        return 60_000_000_000L;
    }

    private static class BarState {
        long token;
        boolean barOpen;
        boolean softClosed;
        boolean subCycleSplitDone;
        long openTime;
        long lastTickTime;
        double open, high, low, close;
        long volume;
        String timeframe;

        BarState(long t, String tf) {
            token = t;
            timeframe = tf;
        }
    }
}
