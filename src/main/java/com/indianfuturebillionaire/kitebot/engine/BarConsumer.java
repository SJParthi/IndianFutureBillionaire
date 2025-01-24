package com.indianfuturebillionaire.kitebot.engine;

import com.indianfuturebillionaire.kitebot.model.Bar;

/**
 * HPC meltdown synergy => interface for bar finalization => strategy, meltdown checks, logging
 */
public interface BarConsumer {
    void onBarFinalized(Bar bar);
}
