package com.indianfuturebillionaire.kitebot.engine;

import com.indianfuturebillionaire.kitebot.config.AggregatorProperties;
import com.indianfuturebillionaire.kitebot.model.Bar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/***********************************************************************
 * HPC meltdown synergy => double buffer ephemeral aggregator =>
 * store ticks in bufferA or bufferB => flip on finalize => minimal rewriting => meltdown skip safe
 */
public class DoubleBufferBarState {

    private static final Logger logger = LoggerFactory.getLogger(DoubleBufferBarState.class);

    private final long token;
    private final String timeframe;

    // HPC meltdown => two ephemeral arrays => flipping
    private final EphemeralArrayBarSegment bufferA;
    private final EphemeralArrayBarSegment bufferB;

    // HPC meltdown => atomic boolean => which buffer we use => start on A
    private final AtomicBoolean usingA = new AtomicBoolean(true);

    public DoubleBufferBarState(long tk, String tf) {
        this.token = tk;
        this.timeframe = tf;
        // HPC meltdown => capacity e.g. 200 => if you expect up to 200 ticks per bar
        this.bufferA = new EphemeralArrayBarSegment(tk, tf, 200);
        this.bufferB = new EphemeralArrayBarSegment(tk, tf, 200);
    }

    /**
     * HPC meltdown synergy => handleTick => ephemeral approach => meltdown skip if meltdown is active
     */
    public void handleTick(double price, long evtNano, AggregatorProperties props, BarConsumer consumer) {
        // HPC meltdown => pick current buffer => store the tick
        EphemeralArrayBarSegment current = usingA.get() ? bufferA : bufferB;
        current.addTick(price, evtNano);

        // HPC meltdown => sub-cycle expansions => check partial bar logic
        if(props.isSubCycle()) {
            maybeSplitBarMidCycle(current, props, consumer);
        }

        // HPC meltdown => check if bar duration exceeded => finalize if so
        long barDur = parseTimeframeNanos(timeframe);
        long elapsed = evtNano - current.getStartNano();
        if(elapsed >= barDur) {
            // HPC meltdown => "soft close" approach => if already flagged => finalize
            if(!current.isSoftClosed()) {
                current.setSoftClosed(true);
            } else {
                finalizeBar(current, props, consumer, "NORMAL");
                flipBuffers();
            }
        }

        // HPC meltdown => partial bar shock => finalize if price moves >= partialBarThreshold
        if(props.isEnablePartialBars()) {
            double openPrice = current.getOpenPrice();
            if(openPrice != 0) {
                double perc = Math.abs(price - openPrice)/openPrice;
                if(perc >= props.getPartialBarThresholdPercent()) {
                    finalizeBar(current, props, consumer, "SHOCK");
                    flipBuffers();
                }
            }
        }
    }

    private void maybeSplitBarMidCycle(EphemeralArrayBarSegment seg, AggregatorProperties props, BarConsumer consumer) {
        long barDur = parseTimeframeNanos(timeframe);
        long halfDur = barDur / 2;
        long now = seg.getLastTickNano();
        long elapsed = now - seg.getStartNano();
        if(!seg.isSubCycleSplitDone() && elapsed > halfDur) {
            finalizeBar(seg, props, consumer, "SUBCYCLE_SPLIT");
            flipBuffers();
        }
    }

    private void finalizeBar(EphemeralArrayBarSegment seg, AggregatorProperties props, BarConsumer consumer, String reason) {
        if(!seg.isActive()) return;
        Bar bar = seg.buildFinalBar();
        bar.setReason(reason);
        consumer.onBarFinalized(bar);
        seg.reset();
    }

    private void flipBuffers() {
        // HPC meltdown => atomic flip => aggregator can continue in new buffer
        boolean wasA = usingA.getAndSet(!usingA.get());
        logger.trace("flipBuffers => wasUsingA={}, nowUsingA={}", wasA, !wasA);
    }

    private long parseTimeframeNanos(String tf) {
        if(tf.endsWith("m")) {
            long mins = Long.parseLong(tf.substring(0, tf.length()-1));
            return mins*60_000_000_000L;
        }
        return 60_000_000_000L;
    }
}
