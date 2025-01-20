package com.indianfuturebillionaire.kitebot.disruptor;

public class TickEvent {
    private long instrumentToken;
    private double lastTradedPrice;
    private long eventNanoTime;
    private long arrivalNanoTime;

    public long getInstrumentToken() { return instrumentToken; }
    public void setInstrumentToken(long token) { instrumentToken=token; }

    public double getLastTradedPrice() { return lastTradedPrice; }
    public void setLastTradedPrice(double p) { lastTradedPrice=p; }

    public long getEventNanoTime() { return eventNanoTime; }
    public void setEventNanoTime(long e) { eventNanoTime=e; }

    public long getArrivalNanoTime() { return arrivalNanoTime; }
    public void setArrivalNanoTime(long a) { arrivalNanoTime=a; }

    public void clear() {
        instrumentToken=0;
        lastTradedPrice=0.0;
        eventNanoTime=0;
        arrivalNanoTime=0;
    }
}
