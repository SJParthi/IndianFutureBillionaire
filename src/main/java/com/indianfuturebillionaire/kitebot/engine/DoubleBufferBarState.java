package com.indianfuturebillionaire.kitebot.engine;

import com.indianfuturebillionaire.kitebot.config.AggregatorProperties;
import com.indianfuturebillionaire.kitebot.model.Bar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/***********************************************************************
 * HPC => double buffer aggregator approach => we store two ephemeral
 * segment arrays (one active, one background).
 * partial expansions => if we do sub-cycle finalize or meltdown,
 * we can flip buffers w/out rewriting fields.
 ***********************************************************************/
public class DoubleBufferBarState {

    private static final Logger logger = LoggerFactory.getLogger(DoubleBufferBarState.class);

    private final long token;
    private final String timeframe;

    // HPC => two ephemeral arrays
    private final EphemeralArrayBarSegment bufferA;
    private final EphemeralArrayBarSegment bufferB;

    private final AtomicBoolean usingA = new AtomicBoolean(true); // start on A

    // HPC => constructor
    public DoubleBufferBarState(long tk, String tf) {
        this.token = tk;
        this.timeframe = tf;
        // ephemeral arrays => capacity could be 100 if we expect ~100 ticks in a bar
        this.bufferA = new EphemeralArrayBarSegment(tk, tf, 200);
        this.bufferB = new EphemeralArrayBarSegment(tk, tf, 200);
    }

    public void handleTick(double price, long evtNano, AggregatorProperties props, BarConsumer consumer) {
        EphemeralArrayBarSegment current = usingA.get() ? bufferA : bufferB;

        // HPC => add tick to ephemeral array
        current.addTick(price, evtNano);

        // partial bar expansions => check if sub-cycle triggered
        if(props.isSubCycle()) {
            maybeSplitBarMidCycle(current, props, consumer);
        }

        // check if bar duration exceeded
        long barDur = parseTimeframeNanos(timeframe);
        long elapsed = evtNano - current.getStartNano();
        if(elapsed >= barDur) {
            // HPC => "soft close" check => if already flagged, finalize
            if(!current.isSoftClosed()) {
                current.setSoftClosed(true);
            } else {
                finalizeBar(current, props, consumer, "NORMAL");
                flipBuffers();
            }
        }

        // partial bar shock
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
        long halfDur = barDur/2;
        long now = seg.getLastTickNano();
        long elapsed = now - seg.getStartNano();
        if(!seg.isSubCycleSplitDone() && elapsed > halfDur) {
            finalizeBar(seg, props, consumer, "SUBCYCLE_SPLIT");
            flipBuffers();
        }
    }

    private void finalizeBar(EphemeralArrayBarSegment seg, AggregatorProperties props, BarConsumer consumer, String reason) {
        if(!seg.isActive()) return; // HPC => if meltdown skip or already final
        Bar bar = seg.buildFinalBar();
        bar.setReason(reason);
        consumer.onBarFinalized(bar);
        seg.reset(); // HPC => ephemeral array cleared
    }

    private void flipBuffers() {
        // HPC => atomic flip => let aggregator continue in new buffer
        boolean wasA = usingA.getAndSet(!usingA.get());
        logger.trace("flipBuffers => wasUsingA={}, nowUsingA={}", wasA, !wasA);
    }

    private long parseTimeframeNanos(String tf) {
        if(tf.endsWith("m")) {
            long mins = Long.parseLong(tf.substring(0, tf.length()-1));
            return mins * 60_000_000_000L;
        }
        return 60_000_000_000L;
    }
}
