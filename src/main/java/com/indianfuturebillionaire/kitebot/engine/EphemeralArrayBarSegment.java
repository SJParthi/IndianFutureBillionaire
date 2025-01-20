package com.indianfuturebillionaire.kitebot.engine;

import com.indianfuturebillionaire.kitebot.model.Bar;

/***********************************************************************
 * HPC ephemeral array => store tick data so we don't rewrite open/high/low
 * every tick. On finalize, we do one pass to get O/H/L/C. meltdown synergy =>
 * aggregator might skip if meltdown is triggered externally.
 ***********************************************************************/
public class EphemeralArrayBarSegment {

    private final long token;
    private final String timeframe;
    private final double[] prices;      // HPC => store price
    private final long[] nanos;        // HPC => store event times
    private int index = 0;
    private boolean active = true;
    private boolean softClosed = false;
    private boolean subCycleSplitDone = false;
    private long startNano = 0;
    private long lastTickNano = 0;

    public EphemeralArrayBarSegment(long tk, String tf, int capacity) {
        this.token = tk;
        this.timeframe = tf;
        this.prices = new double[capacity];
        this.nanos = new long[capacity];
    }

    public void addTick(double price, long evtNano) {
        if(!active) return; // HPC meltdown synergy => skip if not active

        if(index == 0) {
            startNano = evtNano;
        }
        if(index < prices.length) {
            prices[index] = price;
            nanos[index] = evtNano;
            index++;
        }
        lastTickNano = evtNano;
    }

    public Bar buildFinalBar() {
        if(index == 0) {
            // no ticks => return empty bar
            Bar bar = new Bar();
            bar.setInstrumentToken(token);
            bar.setTimeframe(timeframe);
            bar.setOpen(0);
            bar.setHigh(0);
            bar.setLow(0);
            bar.setClose(0);
            bar.setVolume(0);
            bar.setStartTime(startNano);
            bar.setEndTime(startNano);
            setInactive();
            return bar;
        }

        // HPC => single pass to compute O/H/L/C, volume
        double openPrice = prices[0];
        double highPrice = openPrice;
        double lowPrice  = openPrice;
        double closePrice = prices[index-1];
        for(int i=1; i<index; i++) {
            double p = prices[i];
            if(p > highPrice) highPrice = p;
            if(p < lowPrice)  lowPrice = p;
        }
        long volume = index; // HPC => each tick => +1 volume
        Bar bar = new Bar();
        bar.setInstrumentToken(token);
        bar.setTimeframe(timeframe);
        bar.setOpen(openPrice);
        bar.setHigh(highPrice);
        bar.setLow(lowPrice);
        bar.setClose(closePrice);
        bar.setVolume(volume);
        bar.setStartTime(startNano);
        bar.setEndTime(lastTickNano);
        setInactive();
        return bar;
    }

    public void reset() {
        // HPC => reset ephemeral array => ready for next usage
        index = 0;
        active = true;
        softClosed = false;
        subCycleSplitDone = false;
        startNano = 0;
        lastTickNano = 0;
    }

    private void setInactive() {
        active = false;
    }

    // HPC getters
    public boolean isActive() { return active; }
    public boolean isSoftClosed() { return softClosed; }
    public void setSoftClosed(boolean s) { this.softClosed = s; }

    public boolean isSubCycleSplitDone() { return subCycleSplitDone; }
    public void setSubCycleSplitDone(boolean d) { this.subCycleSplitDone = d; }

    public long getStartNano() { return startNano; }
    public long getLastTickNano() { return lastTickNano; }
    public double getOpenPrice() { return (index>0) ? prices[0] : 0; }
}
