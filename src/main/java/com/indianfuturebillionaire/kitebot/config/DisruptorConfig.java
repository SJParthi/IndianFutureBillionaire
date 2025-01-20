package com.indianfuturebillionaire.kitebot.config;

import com.indianfuturebillionaire.kitebot.disruptor.AggregatorEventHandler;
import com.indianfuturebillionaire.kitebot.disruptor.TickEvent;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

/***********************************************************************
 * HPC Disruptor config => we pick a large ring buffer (65536)
 * and use BusySpinWaitStrategy for minimal micro-latency.
 * This ensures no ticks are missed by the aggregator
 * as long as HPC environment can keep up.
 ***********************************************************************/
@Configuration
public class DisruptorConfig {

    // HPC => large ring to avoid overflow
    private static final int RING_BUFFER_SIZE = 65536;

    /**
     * The aggregatorEventHandler is where aggregatorManager.processTick(...)
     * is invoked. If meltdown triggers, aggregator skip logic still reads
     * from ring buffer but does minimal updates, ensuring no ticks are
     * dropped *in code*.
     */
    @Bean
    public Disruptor<TickEvent> tickDisruptor(AggregatorEventHandler aggregatorHandler) {
        Disruptor<TickEvent> disruptor = new Disruptor<>(
                TickEvent::new,              // event factory
                RING_BUFFER_SIZE,
                Executors.defaultThreadFactory(),
                ProducerType.SINGLE,         // single producer => HPC
                new BusySpinWaitStrategy()   // HPC => micro-latency at cost of CPU
        );

        // aggregator event handling => no missed ticks
        disruptor.handleEventsWith(aggregatorHandler);

        disruptor.start();
        return disruptor;
    }
}
