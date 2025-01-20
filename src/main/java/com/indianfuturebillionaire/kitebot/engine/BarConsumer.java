package com.indianfuturebillionaire.kitebot.engine;

import com.indianfuturebillionaire.kitebot.model.Bar;

/****************************************************************
 * Any consumer that wants finalized bars. Could be a strategy,
 * a logs persister, or a partial aggregator chain.
 ****************************************************************/
public interface BarConsumer {
    void onBarFinalized(Bar bar);
}
