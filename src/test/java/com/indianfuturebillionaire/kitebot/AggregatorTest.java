package com.indianfuturebillionaire.kitebot;

import com.indianfuturebillionaire.kitebot.disruptor.TickEventProducer;
import com.indianfuturebillionaire.kitebot.engine.MultiTimeframeAggregatorManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AggregatorTest {

    @Autowired
    private MultiTimeframeAggregatorManager aggregatorManager;

    @Autowired
    private TickEventProducer tickProducer;

    @Test
    void testShockBar() {
        // direct call aggregator or we can do it via tickProducer
        aggregatorManager.processTick(123L, 100.0, 1_000_000_000L, 1_000_000_000L);
        aggregatorManager.processTick(123L, 150.0, 2_000_000_000L, 2_000_000_000L);
        // aggregator should finalize bar with reason="SHOCK"
    }
}
