package com.indianfuturebillionaire.kitebot.service;

import com.indianfuturebillionaire.kitebot.feed.WebSocketManager;
import com.indianfuturebillionaire.kitebot.risk.RiskManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/****************************************************************
 * At 4pm => stop feed, reset daily risk, aggregator flush if needed.
 ****************************************************************/
@Service
public class MarketCloseScheduler {

    private static final Logger logger = LoggerFactory.getLogger(MarketCloseScheduler.class);

    private final WebSocketManager wsManager;
    private final RiskManagerService riskManager;
    // aggregator manager if you want to flush

    public MarketCloseScheduler(WebSocketManager w, RiskManagerService r) {
        this.wsManager = w;
        this.riskManager= r;
    }

    @Scheduled(cron="0 0 16 * * MON-FRI", zone="Asia/Kolkata")
    public void onMarketClose() {
        logger.info("Market close => stopping websockets, resetting daily risk");
        wsManager.stopAllWebSockets();
        riskManager.resetDailyCount();
        // aggregator flush if needed
    }
}
