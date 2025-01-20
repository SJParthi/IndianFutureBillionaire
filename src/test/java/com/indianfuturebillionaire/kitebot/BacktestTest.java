package com.indianfuturebillionaire.kitebot;

import com.indianfuturebillionaire.kitebot.engine.BacktestRunnerService;
import com.indianfuturebillionaire.kitebot.engine.BacktestRunnerService.HistoricalTick;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class BacktestTest {

    @Autowired
    private BacktestRunnerService backtestRunner;

    @Test
    void testRunBacktest() {
        List<HistoricalTick> ticks = new ArrayList<>();
        ticks.add(new HistoricalTick(123L, 100.0, 1_000_000_000L,1_000_000_000L));
        ticks.add(new HistoricalTick(123L, 101.0, 2_000_000_000L,2_000_000_000L));
        // etc.

        backtestRunner.runBacktest(ticks);
    }
}
