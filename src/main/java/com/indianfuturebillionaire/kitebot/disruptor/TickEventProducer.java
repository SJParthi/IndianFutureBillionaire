package com.indianfuturebillionaire.kitebot.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.stereotype.Component;

@Component
public class TickEventProducer {

    private final RingBuffer<TickEvent> ringBuffer;

    public TickEventProducer(Disruptor<TickEvent> disruptor) {
        this.ringBuffer = disruptor.getRingBuffer();
    }

    public void publishTick(long token, double price, long eventTime, long arrivalTime) {
        long seq = ringBuffer.next();
        try {
            TickEvent evt = ringBuffer.get(seq);
            evt.setInstrumentToken(token);
            evt.setLastTradedPrice(price);
            evt.setEventNanoTime(eventTime);
            evt.setArrivalNanoTime(arrivalTime);
        } finally {
            ringBuffer.publish(seq);
        }
    }
}
