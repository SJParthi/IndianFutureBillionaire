package com.indianfuturebillionaire.kitebot.engine;

import com.indianfuturebillionaire.kitebot.model.Bar;
import com.indianfuturebillionaire.kitebot.risk.RiskManagerService;
import com.indianfuturebillionaire.kitebot.service.MarketDataService;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * HPC meltdown synergy => final bar consumer => ephemeral aggregator => meltdown skip => updates MarketDataService
 */
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
        logger.debug("onBarFinalized => ephemeral aggregator => token={}, tf={}, reason={}, O/H/L/C={}/{}/{}/{}, vol={}",
                bar.getInstrumentToken(), bar.getCompanyName(), bar.getTimeframe(), bar.getReason(),
                bar.getOpen(), bar.getHigh(), bar.getLow(), bar.getClose(), bar.getVolume());

        // HPC meltdown synergy => update MarketDataService => used by dashboard => top gainers/losers
        marketDataService.updateLtp(bar.getInstrumentToken(), bar.getClose());
        marketDataService.updateVolume(bar.getInstrumentToken(), bar.getVolume());
        // if you have a known prevClose from somewhere => setPrevClose(...) once a day or so

        // meltdown on volume => HPC synergy => aggregator skip
        if(bar.getVolume()> meltdownThreshold && !riskManager.isMeltdownActive()) {
            logger.warn("barVolume={} > meltdownThreshold={}, meltdown triggered => token={}",
                    bar.getVolume(), meltdownThreshold, bar.getInstrumentToken());
            riskManager.activateMeltdownMode();
        }
    }
}
