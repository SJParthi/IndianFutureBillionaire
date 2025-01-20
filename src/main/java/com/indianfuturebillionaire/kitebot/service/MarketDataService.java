package com.indianfuturebillionaire.kitebot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/***********************************************************************
 * HPC meltdown aggregator =>
 * MarketDataService is where we store real-time LTP (last traded price),
 * previous close, volume for each instrumentToken or symbol,
 * then compute top gainers, losers, volume, intraday for your dashboard.
 *
 * This version includes the "updateLtp(long,double)" method so
 * "marketDataService.updateLtp(bar.getInstrumentToken(), bar.getClose())"
 * compiles properly in StrategyBarConsumer or aggregator code.
 ***********************************************************************/
@Service
public class MarketDataService {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);

    // HPC approach => store LTP keyed by instrumentToken (long)
    private final ConcurrentHashMap<Long, Double> ltpMap = new ConcurrentHashMap<>();

    // optional => if you want to compute %change from a previous close
    private final ConcurrentHashMap<Long, Double> prevCloseMap = new ConcurrentHashMap<>();

    // store volumes keyed by instrumentToken
    private final ConcurrentHashMap<Long, Long> volumeMap = new ConcurrentHashMap<>();

    public MarketDataService() {
        logger.info("MarketDataService => created");
    }

    /***********************************************************************
     * HPC meltdown synergy => aggregator code calls this each time
     * a bar finalizes or a tick arrives with a new LTP.
     * "token" is your instrumentToken, "price" is the new LTP or bar close.
     ***********************************************************************/
    public void updateLtp(long token, double price) {
        ltpMap.put(token, price);
    }

    /***********************************************************************
     * If you track a "previous close," call this method once you know
     * the previous day's close or from aggregator logic.
     ***********************************************************************/
    public void setPrevClose(long token, double closePrice) {
        prevCloseMap.put(token, closePrice);
    }

    /***********************************************************************
     * HPC meltdown synergy => aggregator or meltdown logic might update
     * volumes in real time (especially if you track partial bar volumes).
     ***********************************************************************/
    public void updateVolume(long token, long vol) {
        volumeMap.put(token, vol);
    }

    /***********************************************************************
     * HPC meltdown synergy => top gainers => sorted by %change descending
     * We compute %change = (ltp - prevClose)/prevClose * 100.
     * If no prevClose is known, skip or treat it as 0 => no computation.
     ***********************************************************************/
    public List<StockChange> getTopGainers(int limit) {
        List<StockChange> list = new ArrayList<>();
        for(Map.Entry<Long, Double> e : ltpMap.entrySet()) {
            long token = e.getKey();
            double ltp = e.getValue();
            double pc  = prevCloseMap.getOrDefault(token, 0.0);
            long vol   = volumeMap.getOrDefault(token, 0L);
            if(pc>0.0) {
                double pct = (ltp - pc)/pc * 100.0;
                list.add(new StockChange(token, ltp, pc, pct, vol));
            }
        }
        // sort descending by %change
        list.sort((a,b)-> Double.compare(b.percentChange, a.percentChange));
        if(list.size()>limit) {
            return list.subList(0, limit);
        }
        return list;
    }

    /***********************************************************************
     * HPC meltdown synergy => top losers => sorted by %change ascending
     ***********************************************************************/
    public List<StockChange> getTopLosers(int limit) {
        List<StockChange> list = new ArrayList<>();
        for(Map.Entry<Long, Double> e : ltpMap.entrySet()) {
            long token = e.getKey();
            double ltp = e.getValue();
            double pc  = prevCloseMap.getOrDefault(token, 0.0);
            long vol   = volumeMap.getOrDefault(token, 0L);
            if(pc>0.0) {
                double pct = (ltp - pc)/pc * 100.0;
                list.add(new StockChange(token, ltp, pc, pct, vol));
            }
        }
        // sort ascending => biggest negative first
        list.sort((a,b)-> Double.compare(a.percentChange, b.percentChange));
        if(list.size()>limit) {
            return list.subList(0, limit);
        }
        return list;
    }

    /***********************************************************************
     * HPC meltdown synergy => top by volume => sorted descending by volume
     ***********************************************************************/
    public List<StockChange> getTopByVolume(int limit) {
        List<StockChange> list = new ArrayList<>();
        for(Map.Entry<Long, Long> e : volumeMap.entrySet()) {
            long token = e.getKey();
            long vol   = e.getValue();
            double ltp = ltpMap.getOrDefault(token, 0.0);
            double pc  = prevCloseMap.getOrDefault(token, 0.0);
            double pct = 0.0;
            if(pc>0.0) {
                pct = (ltp - pc)/pc * 100.0;
            }
            list.add(new StockChange(token, ltp, pc, pct, vol));
        }
        list.sort((a,b)-> Long.compare(b.volume, a.volume));
        if(list.size()>limit) {
            return list.subList(0, limit);
        }
        return list;
    }

    /***********************************************************************
     * HPC meltdown synergy => top intraday => logic is up to you.
     * For demonstration, let's reuse top gainers logic.
     ***********************************************************************/
    public List<StockChange> getTopIntraday(int limit) {
        // or define your own intraday logic
        return getTopGainers(limit);
    }

    /***********************************************************************
     * HPC meltdown synergy => a data class representing an instrument's
     * real-time changes, used in your dashboard.
     ***********************************************************************/
    public static class StockChange {
        public long token;
        public double ltp;
        public double prevClose;
        public double percentChange;
        public long volume;

        public StockChange(long token, double ltp, double pc, double pct, long vol) {
            this.token = token;
            this.ltp   = ltp;
            this.prevClose = pc;
            this.percentChange = pct;
            this.volume = vol;
        }
    }
}
