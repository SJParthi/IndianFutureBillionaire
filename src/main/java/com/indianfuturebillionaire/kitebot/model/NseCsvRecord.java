package com.indianfuturebillionaire.kitebot.model;

/**
 * Represents a row from an NSE CSV (e.g. for NIFTY 500).
 */
public class NseCsvRecord {
    private String companyName;
    private String industry;
    private String symbol;
    private String series;
    private String isin;

    public NseCsvRecord(String companyName, String industry, String symbol, String series, String isin) {
        this.companyName = companyName;
        this.industry = industry;
        this.symbol = symbol;
        this.series = series;
        this.isin = isin;
    }

    // Getters & Setters
    public String getSymbol() {
        return symbol;
    }
}
