package com.indianfuturebillionaire.kitebot.config;

import com.indianfuturebillionaire.kitebot.disruptor.AggregatorEventHandler;
import com.indianfuturebillionaire.kitebot.disruptor.TickEvent;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

/**
 * HPC meltdown synergy => configures LMAX Disruptor with ring buffer size from aggregator properties.
 * Ensures minimal-latency => BusySpinWaitStrategy, can handle thousands of ticks/sec for sub-10ms usage.
 */
@Configuration
public class DisruptorConfig {

    /**
     * Creates the Disruptor bean => HPC meltdown synergy => ring buffer consumer => aggregator event handler
     */
    @Bean
    public Disruptor<TickEvent> tickDisruptor(AggregatorProperties props,
                                              AggregatorEventHandler aggregatorHandler) {
        // HPC meltdown synergy => ring size from aggregator config => e.g. 65536
        int ringSize = props.getRingBufferSize();

        // Build disruptor => single producer or multi => if multiple websockets => use MULTI
        Disruptor<TickEvent> disruptor = new Disruptor<>(
                TickEvent::new,                // event factory => HPC meltdown synergy minimal overhead
                ringSize,
                Executors.defaultThreadFactory(),
                ProducerType.SINGLE,           // HPC meltdown synergy => single or multi
                new BusySpinWaitStrategy()     // HPC meltdown synergy => minimal-latency wait strategy
        );

        // aggregatorEventHandler => consumes TickEvents => meltdown synergy aggregator
        disruptor.handleEventsWith(aggregatorHandler);

        // Start the ring buffer consumption
        disruptor.start();
        return disruptor;
    }
}
