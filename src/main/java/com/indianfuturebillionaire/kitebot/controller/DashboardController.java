package com.indianfuturebillionaire.kitebot.controller;

import com.indianfuturebillionaire.kitebot.engine.DoubleBufferAggregatorManager;
import com.indianfuturebillionaire.kitebot.risk.RiskManagerService;
import com.indianfuturebillionaire.kitebot.feed.WebSocketManager;
import com.indianfuturebillionaire.kitebot.service.MarketDataService;
import com.indianfuturebillionaire.kitebot.service.MarketDataService.StockChange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller; // HPC meltdown => must be @Controller, not @RestController
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * HPC meltdown synergy => aggregator front-end => shows meltdown states, aggregator usage,
 * top gainers/losers from ephemeral aggregator => MarketDataService => meltdown synergy
 */
@Controller
public class DashboardController {

    private final MarketDataService marketDataService;
    private final RiskManagerService riskManager;
    private final DoubleBufferAggregatorManager aggregatorManager;
    private WebSocketManager wsManager;

    @Value("${indexes.preload:}")
    private List<String> preloadedIndexes;

    @Autowired
    public void setMultipleWebSocketManager(WebSocketManager wsm) {
        this.wsManager = wsm;
    }

    @Autowired
    public DashboardController(MarketDataService mds,
                               RiskManagerService rm,
                               DoubleBufferAggregatorManager am) {
        this.marketDataService = mds;
        this.riskManager = rm;
        this.aggregatorManager = am;
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        // meltdown state => meltdown skip or aggregator normal
        boolean meltdownActive = riskManager.isMeltdownActive();

        // aggregator usage => ringBuf usage, feed rate => ephemeral aggregator synergy
        double ringBufUsagePct = aggregatorManager.getRingBufferUsagePercent();
        long feedRate = aggregatorManager.getRecentTickRate();

        // meltdown logs => ephemeral aggregator concurrency stats
        List<String> meltdownLogs = aggregatorManager.getRecentMeltdownLogs();
        String concurrencyStatsJson = aggregatorManager.getConcurrencyStatsJson(); // partial meltdown stats if you store them

        // HPC meltdown synergy => top gainers, losers, volume, intraday => from MarketDataService
        List<StockChange> topGainers  = marketDataService.getTopGainers(50);
        List<StockChange> topLosers   = marketDataService.getTopLosers(50);
        List<StockChange> topVolume   = marketDataService.getTopByVolume(50);
        List<StockChange> topIntraday = marketDataService.getTopIntraday(50);

        // HPC meltdown synergy => websockets => meltdown synergy => let user see active indexes
        int websocketCount = wsManager.getActiveWebSocketCount();
        List<String> activeIndexes = wsManager.getActiveIndexes();

        // HPC meltdown synergy => put them in thymeleaf model
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

        // HPC meltdown synergy => returns thymeleaf template => "dashboard"
        return "dashboard";
    }
}
