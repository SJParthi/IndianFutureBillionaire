package com.indianfuturebillionaire.kitebot.model;

public class Bar {
    private long instrumentToken;
    private String timeframe;
    private double open, high, low, close;
    private long volume;
    private long startTime;
    private long endTime;
    private String reason;

    public long getInstrumentToken() { return instrumentToken; }
    public void setInstrumentToken(long t) { instrumentToken=t; }

    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String tf) { timeframe=tf; }

    public double getOpen() { return open; }
    public void setOpen(double o) { open=o; }

    public double getHigh() { return high; }
    public void setHigh(double h) { high=h; }

    public double getLow() { return low; }
    public void setLow(double l) { low=l; }

    public double getClose() { return close; }
    public void setClose(double c) { close=c; }

    public long getVolume() { return volume; }
    public void setVolume(long v) { volume=v; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long s) { startTime=s; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long e) { endTime=e; }

    public String getReason() { return reason; }
    public void setReason(String r) { reason=r; }
}
