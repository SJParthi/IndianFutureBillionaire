package com.indianfuturebillionaire.kitebot.engine;

import com.indianfuturebillionaire.kitebot.model.Bar;
import com.indianfuturebillionaire.kitebot.risk.RiskManagerService;
import com.indianfuturebillionaire.kitebot.service.MarketDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/***********************************************************************
 * HPC bar consumer => logs final bars, updates real-time LTP,
 * triggers meltdown if volume crosses meltdownThreshold
 * or if ring buffer usage is too high (optional).
 ***********************************************************************/
@Service
public class StrategyBarConsumer implements BarConsumer {

    private static final Logger logger = LoggerFactory.getLogger(StrategyBarConsumer.class);

    private final RiskManagerService riskManager;
    private final MarketDataService marketDataService;
    private final int meltdownThreshold;

    public StrategyBarConsumer(RiskManagerService rm,
                               MarketDataService mds,
                               @Value("${risk.meltdown-threshold}") int meltdownThreshold) {
        this.riskManager = rm;
        this.marketDataService = mds;
        this.meltdownThreshold = meltdownThreshold;
    }

    @Override
    public void onBarFinalized(Bar bar) {
        logger.debug("onBarFinalized => token={}, tf={}, reason={}, O/H/L/C={}/{}/{}/{}, vol={}",
                bar.getInstrumentToken(), bar.getTimeframe(), bar.getReason(),
                bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose(), bar.getVolume());

        // HPC => update real-time LTP => used by dashboard for top gainers/losers
        marketDataService.updateLtp(bar.getInstrumentToken(), bar.getClose());

        // meltdown on volume
        if(bar.getVolume() > meltdownThreshold && !riskManager.isMeltdownActive()) {
            logger.warn("barVolume={} > meltdownThreshold={}, meltdown triggered => token={}",
                    bar.getVolume(), meltdownThreshold, bar.getInstrumentToken());
            riskManager.activateMeltdownMode();
        }
    }
}
