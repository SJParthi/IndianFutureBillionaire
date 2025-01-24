package com.indianfuturebillionaire.kitebot.model;

/**
 * HPC meltdown synergy => final bar model => minimal overhead => real-time aggregator synergy
 */
public class Bar {
    private long instrumentToken;
    public String companyName;    // new field
    private String timeframe;
    private double open, high, low, close;
    private long volume;
    private long startTime, endTime;
    private String reason;

    // HPC meltdown synergy => getters & setters => O(1)
    public long getInstrumentToken() { return instrumentToken; }
    public void setInstrumentToken(long instrumentToken) { this.instrumentToken = instrumentToken; }

    public String getCompanyName() {
        return companyName;
    }
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }

    public double getOpen() { return open; }
    public void setOpen(double open) { this.open = open; }

    public double getHigh() { return high; }
    public void setHigh(double high) { this.high = high; }

    public double getLow() { return low; }
    public void setLow(double low) { this.low = low; }

    public double getClose() { return close; }
    public void setClose(double close) { this.close = close; }

    public long getVolume() { return volume; }
    public void setVolume(long volume) { this.volume = volume; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
