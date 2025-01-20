package com.indianfuturebillionaire.kitebot.controller;

import com.indianfuturebillionaire.kitebot.feed.MultipleWebSocketManager;
import com.indianfuturebillionaire.kitebot.engine.MultiTimeframeAggregatorManager;
import com.indianfuturebillionaire.kitebot.risk.RiskManagerService;
import com.indianfuturebillionaire.kitebot.service.MarketDataService;
import com.indianfuturebillionaire.kitebot.service.MarketDataService.StockChange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/***********************************************************************
 * HPC meltdown aggregator front-end controller.
 *
 *  - Gathers meltdown states (riskManager)
 *  - Aggregator usage (aggregatorManager => feed rate, ring buffer usage, meltdown logs)
 *  - Market data for top gainers/losers/volume/intraday (marketDataService)
 *  - WebSocket info (# connections, active indexes)
 *  - Renders "dashboard.html" with fancy JS/CSS visuals.
 ***********************************************************************/
@RestController
public class DashboardController {

    private final MarketDataService marketDataService;
    private final RiskManagerService riskManager;
    private final MultiTimeframeAggregatorManager aggregatorManager;

    // Instead of final injection, do a setter or no injection at all.
    // Only inject if truly necessary:
    private MultipleWebSocketManager wsManager;

    @Autowired
    public void setMultipleWebSocketManager(MultipleWebSocketManager wsm) {
        this.wsManager = wsm;
    }

    @Value("${indexes.preload:}")
    private List<String> preloadedIndexes; // e.g. ["NIFTY 50", "NIFTY 100", "NIFTY 500"]

    public DashboardController(MarketDataService mds,
                               RiskManagerService rm,
                               MultiTimeframeAggregatorManager am) {
        this.marketDataService = mds;
        this.riskManager = rm;
        this.aggregatorManager = am;
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        // meltdown state
        boolean meltdownActive = riskManager.isMeltdownActive();

        // aggregator ring buffer usage, feed rate
        double ringBufUsagePct = aggregatorManager.getRingBufferUsagePercent();
        long feedRate = aggregatorManager.getRecentTickRate();

        // meltdown logs or concurrency stats => optional
        List<String> meltdownLogs = aggregatorManager.getRecentMeltdownLogs();
        String concurrencyStatsJson = aggregatorManager.getConcurrencyStatsJson();

        // top gainer/loser/volume/intraday
        List<StockChange> topGainers  = marketDataService.getTopGainers(50);
        List<StockChange> topLosers   = marketDataService.getTopLosers(50);
        List<StockChange> topVolume   = marketDataService.getTopByVolume(50);
        List<StockChange> topIntraday = marketDataService.getTopIntraday(50);

        // websockets
        int websocketCount = wsManager.getActiveWebSocketCount();
        List<String> activeIndexes = wsManager.getActiveIndexes();

        // add to Thymeleaf model
        model.addAttribute("meltdownActive", meltdownActive);
        model.addAttribute("ringBufUsagePct", ringBufUsagePct);
        model.addAttribute("feedRate", feedRate);
        model.addAttribute("meltdownLogs", meltdownLogs);
        model.addAttribute("concurrencyStatsJson", concurrencyStatsJson);

        model.addAttribute("topGainers", topGainers);
        model.addAttribute("topLosers", topLosers);
        model.addAttribute("topVolume", topVolume);
        model.addAttribute("topIntraday", topIntraday);

        model.addAttribute("websocketCount", websocketCount);
        model.addAttribute("activeIndexes", activeIndexes);
        model.addAttribute("preloadedIndexes", preloadedIndexes);

        return "dashboard";
    }
}
