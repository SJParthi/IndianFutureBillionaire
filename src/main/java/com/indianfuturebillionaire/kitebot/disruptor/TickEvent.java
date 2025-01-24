package com.indianfuturebillionaire.kitebot.disruptor;

/**
 * HPC meltdown synergy => minimal TickEvent stored in ring buffer => O(1) overhead
 */
public class TickEvent {
    private long instrumentToken;
    private double lastTradedPrice;
    private long eventNanoTime;
    private long arrivalNanoTime;

    // HPC meltdown synergy => standard getters & setters
    public long getInstrumentToken() { return instrumentToken; }
    public void setInstrumentToken(long t) { this.instrumentToken = t; }

    public double getLastTradedPrice() { return lastTradedPrice; }
    public void setLastTradedPrice(double p) { this.lastTradedPrice = p; }

    public long getEventNanoTime() { return eventNanoTime; }
    public void setEventNanoTime(long e) { this.eventNanoTime = e; }

    public long getArrivalNanoTime() { return arrivalNanoTime; }
    public void setArrivalNanoTime(long a) { this.arrivalNanoTime = a; }

    /**
     * HPC meltdown synergy => clears fields => ring buffer can reuse event slot
     */
    public void clear() {
        instrumentToken = 0;
        lastTradedPrice = 0.0;
        eventNanoTime = 0;
        arrivalNanoTime = 0;
    }
}
