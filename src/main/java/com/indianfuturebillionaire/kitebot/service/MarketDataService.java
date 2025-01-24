package com.indianfuturebillionaire.kitebot.service;

import org.slf4j.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HPC meltdown synergy => store real-time LTP, volume => aggregator -> StrategyBarConsumer -> MarketDataService => dashboard
 */
@Service
public class MarketDataService {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataService.class);

    // HPC meltdown synergy => store LTP keyed by token
    private final ConcurrentHashMap<Long, Double> ltpMap = new ConcurrentHashMap<>();
    // HPC meltdown synergy => store previous close => for %change
    private final ConcurrentHashMap<Long, Double> prevCloseMap = new ConcurrentHashMap<>();
    // HPC meltdown synergy => store volume => aggregator sets final bar volume
    private final ConcurrentHashMap<Long, Long> volumeMap = new ConcurrentHashMap<>();

    public MarketDataService() {
        logger.info("MarketDataService => HPC meltdown synergy => created");
    }

    public void updateLtp(long token, double price) {
        ltpMap.put(token, price);
    }

    public void setPrevClose(long token, double pc) {
        prevCloseMap.put(token, pc);
    }

    public void updateVolume(long token, long vol) {
        volumeMap.put(token, vol);
    }

    // HPC meltdown synergy => top gainers => sort by %change desc
    public List<StockChange> getTopGainers(int limit) {
        List<StockChange> out = new ArrayList<>();
        for(Map.Entry<Long,Double> e : ltpMap.entrySet()) {
            long t = e.getKey();
            double ltp = e.getValue();
            double pc  = prevCloseMap.getOrDefault(t, 0.0);
            long v = volumeMap.getOrDefault(t,0L);
            if(pc>0) {
                double pct = (ltp - pc)/pc *100.0;
                out.add(new StockChange("Token-"+t, "caompanyNameHardcoded",ltp, pc, pct, v));
            }
        }
        out.sort((a,b)-> Double.compare(b.percentChange, a.percentChange));
        if(out.size()>limit) return out.subList(0,limit);
        return out;
    }

    // HPC meltdown synergy => top losers => sort ascending by %change
    public List<StockChange> getTopLosers(int limit) {
        List<StockChange> out = new ArrayList<>();
        for(Map.Entry<Long,Double> e : ltpMap.entrySet()) {
            long t = e.getKey();
            double ltp = e.getValue();
            double pc  = prevCloseMap.getOrDefault(t, 0.0);
            long v = volumeMap.getOrDefault(t,0L);
            if(pc>0) {
                double pct = (ltp - pc)/pc*100.0;
                out.add(new StockChange("Token-"+t, "caompanyNameHardcoded",ltp, pc, pct, v));
            }
        }
        out.sort((a,b)-> Double.compare(a.percentChange,b.percentChange));
        if(out.size()>limit) return out.subList(0,limit);
        return out;
    }

    // HPC meltdown synergy => top by volume => sort descending by volume
    public List<StockChange> getTopByVolume(int limit) {
        List<StockChange> out = new ArrayList<>();
        for(Map.Entry<Long, Long> e : volumeMap.entrySet()) {
            long t = e.getKey();
            long vol = e.getValue();
            double ltp = ltpMap.getOrDefault(t,0.0);
            double pc  = prevCloseMap.getOrDefault(t,0.0);
            double pct = 0;
            if(pc>0) pct = (ltp - pc)/pc *100.0;
            out.add(new StockChange("Token-"+t, "caompanyNameHardcoded",ltp, pc, pct, vol));
        }
        out.sort((a,b)-> Long.compare(b.volume,a.volume));
        if(out.size()>limit) return out.subList(0,limit);
        return out;
    }

    // HPC meltdown synergy => top intraday => e.g. reuse top gainers logic
    public List<StockChange> getTopIntraday(int limit) {
        return getTopGainers(limit);
    }

    /**
     * HPC meltdown synergy => data object => matches thymeleaf => sc.symbolOrToken, sc.ltp, sc.prevClose, sc.percentChange, sc.volume
     */
    public static class StockChange {
        public String symbolOrToken;
        public String companyName;    // new field
        public double ltp;
        public double prevClose;
        public double percentChange;
        public long volume;

        public StockChange(String symbolOrToken, String companyName, double ltp, double pc, double pct, long vol) {
            this.symbolOrToken = symbolOrToken;
            this.companyName = companyName;
            this.ltp = ltp;
            this.prevClose = pc;
            this.percentChange = pct;
            this.volume = vol;
        }
    }
}
