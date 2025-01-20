package com.indianfuturebillionaire.kitebot.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/****************************************************************
 * Replays historical ticks into aggregator => identical code
 * to real usage, guaranteeing consistent backtest.
 ****************************************************************/
@Service
public class BacktestRunnerService {

    private static final Logger logger = LoggerFactory.getLogger(BacktestRunnerService.class);

    private final MultiTimeframeAggregatorManager aggregatorManager;

    public BacktestRunnerService(MultiTimeframeAggregatorManager mgr) {
        this.aggregatorManager = mgr;
    }

    public void runBacktest(List<HistoricalTick> ticks) {
        logger.info("Backtest started => total ticks={}", ticks.size());
        ticks.sort((a,b)-> Long.compare(a.eventNanoTime, b.eventNanoTime));
        for (HistoricalTick ht : ticks) {
            aggregatorManager.processTick(
                    ht.instrumentToken,
                    ht.price,
                    ht.eventNanoTime,
                    ht.arrivalNanoTime
            );
        }
        logger.info("Backtest ended => aggregator may not finalize all bars if day boundary not forced.");
    }

    public static class HistoricalTick {
        public long instrumentToken;
        public double price;
        public long eventNanoTime;
        public long arrivalNanoTime;

        public HistoricalTick(long t, double p, long e, long a) {
            instrumentToken = t;
            price = p;
            eventNanoTime = e;
            arrivalNanoTime = a;
        }
    }
}
