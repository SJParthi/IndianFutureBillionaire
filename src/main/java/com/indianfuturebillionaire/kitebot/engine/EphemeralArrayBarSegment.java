package com.indianfuturebillionaire.kitebot.engine;

import com.indianfuturebillionaire.kitebot.model.Bar;

/***********************************************************************
 * HPC meltdown synergy => ephemeral array => store tick data in an array.
 * On finalize, do one pass to compute O/H/L/C, volume => meltdown skip if meltdown.
 */
public class EphemeralArrayBarSegment {

    private final long token;         // HPC meltdown => instrument token
    private final String timeframe;   // HPC meltdown => timeframe, e.g. "1m"

    private final double[] prices;    // HPC meltdown => store prices for each tick
    private final long[] nanos;       // HPC meltdown => store event times

    private int index = 0;           // HPC meltdown => how many ticks stored
    private boolean active = true;   // HPC meltdown => if meltdown skip => might set inactive
    private boolean softClosed = false;
    private boolean subCycleSplitDone = false;

    private long startNano = 0;      // HPC meltdown => time of first tick
    private long lastTickNano = 0;   // HPC meltdown => time of last tick

    public EphemeralArrayBarSegment(long tk, String tf, int capacity) {
        this.token = tk;
        this.timeframe = tf;
        // HPC meltdown => capacity e.g. 200 => if bar sees more than 200 ticks, we must handle overflow
        this.prices = new double[capacity];
        this.nanos = new long[capacity];
    }

    /**
     * HPC meltdown synergy => store the new price/time => if meltdown is triggered externally => skip or partial skip
     */
    public void addTick(double price, long evtNano) {
        if(!active) return; // HPC meltdown => aggregator skip => do nothing

        if(index == 0) {
            // HPC meltdown => first tick => record startNano
            startNano = evtNano;
        }
        // HPC meltdown => if index >= capacity => we might do partial meltdown skip or finalize early
        if(index < prices.length) {
            prices[index] = price;
            nanos[index] = evtNano;
            index++;
        }
        lastTickNano = evtNano;
    }

    /**
     * HPC meltdown synergy => build final bar => single pass => O(N) => meltdown skip can reduce frequency
     */
    public Bar buildFinalBar() {
        if(index == 0) {
            // HPC meltdown => no ticks => return empty bar
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

        // HPC meltdown synergy => single pass to compute O/H/L => index ticks
        double openPrice = prices[0];
        double highPrice = openPrice;
        double lowPrice  = openPrice;
        double closePrice = prices[index-1];
        for(int i=1; i<index; i++) {
            double p = prices[i];
            if(p > highPrice) highPrice = p;
            if(p < lowPrice)  lowPrice = p;
        }
        // HPC meltdown => volume = # of ticks => index
        long volume = index;

        // HPC meltdown => build final bar => meltdown skip if meltdown
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

    /**
     * HPC meltdown synergy => reset ephemeral array => ready for new bar
     */
    public void reset() {
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

    // HPC meltdown synergy => getters
    public boolean isActive() { return active; }
    public boolean isSoftClosed() { return softClosed; }
    public void setSoftClosed(boolean s) { this.softClosed = s; }

    public boolean isSubCycleSplitDone() { return subCycleSplitDone; }
    public void setSubCycleSplitDone(boolean d) { this.subCycleSplitDone = d; }

    public long getStartNano() { return startNano; }
    public long getLastTickNano() { return lastTickNano; }
    public double getOpenPrice() {
        if(index>0) return prices[0];
        return 0;
    }
}
