package com.indianfuturebillionaire.kitebot.feed;

import com.indianfuturebillionaire.kitebot.disruptor.TickEventProducer;
import com.indianfuturebillionaire.kitebot.model.NseCsvRecord;
import com.indianfuturebillionaire.kitebot.service.IndexCsvDownloadService;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Tick;
import com.zerodhatech.ticker.*;
import org.slf4j.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HPC meltdown synergy => single subscription => no chunking =>
 * Aggressive reconnection => sub-10ms if feasible => meltdown aggregator
 */
@Service
public class WebSocketManager {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketManager.class);

    private final TickEventProducer producer;
    private final KiteConnect kiteConnect;
    private final IndexCsvDownloadService indexCsvDownloadService;

    // HPC meltdown synergy => store references => if meltdown => aggregator skip => feed remains same
    private final List<KiteTicker> tickers = new ArrayList<>();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final Set<String> activeIndexes = new HashSet<>();

    // HPC meltdown synergy => minimal recon delays => sub-10ms example => might do partial backoff
    private final long initialReconnectDelayMillis = 10;

    public WebSocketManager(TickEventProducer producer,
                            KiteConnect kiteConnect,
                            IndexCsvDownloadService indexCsvDownloadService) {
        this.producer = producer;
        this.kiteConnect = kiteConnect;
        this.indexCsvDownloadService = indexCsvDownloadService;
    }

    /**
     * HPC meltdown synergy => start websockets => single subscription => sub-10ms recon attempt
     */
    public synchronized void startWebSocketsForIndex(String indexName) {
        logger.info("startWebSocketsForIndex => index={}, startedPreviously={}", indexName, started.get());
        if(started.get()) {
            stopAllWebSockets();
        }
        try {
            List<Long> tokens = fetchTokensForIndex(indexName);
            if(tokens.isEmpty()) {
                logger.warn("No tokens => skipping start => index={}", indexName);
                return;
            }
            KiteTicker ticker = new KiteTicker(kiteConnect.getAccessToken(), kiteConnect.getApiKey());
            registerHandlers(ticker, new ArrayList<>(tokens), indexName);
            tickers.add(ticker);
            ticker.connect();
            started.set(true);
            activeIndexes.clear();
            activeIndexes.add(indexName);
            logger.info("WebSockets => index={}, totalTickers={}, totalTokens={}",
                    indexName, tickers.size(), tokens.size());
        } catch(Exception e) {
            logger.error("startWebSocketsForIndex => error => index={}", indexName, e);
        }
    }

    /**
     * HPC meltdown synergy => stop all websockets => meltdown or manual => O(tickers.size())
     */
    public synchronized void stopAllWebSockets() {
        logger.info("stopAllWebSockets => totalTickers={}", tickers.size());
        for(KiteTicker kt : tickers) {
            if(kt != null) {
                try {
                    kt.disconnect();
                } catch(Exception e) {
                    logger.warn("Error while disconnecting => ", e);
                }
            }
        }
        tickers.clear();
        activeIndexes.clear();
        started.set(false);
        logger.info("All websockets disconnected => meltdown synergy aggregator can idle");
    }

    /**
     * HPC meltdown synergy => fetch tokens => e.g. from CSV => might have thousands => aggregator meltdown skip if overload
     */
    private List<Long> fetchTokensForIndex(String indexName) throws Exception {
        List<NseCsvRecord> records = indexCsvDownloadService.downloadIndexCsv(indexName);
        List<Long> tokens = indexCsvDownloadService.mapRecordsToTokens(records);
        logger.info("fetchTokensForIndex => index={}, totalTokens={}", indexName, tokens.size());
        return tokens;
    }

    /**
     * HPC meltdown synergy => register handlers => single subscription => if disconnected => reconnect quickly
     */
    private void registerHandlers(KiteTicker ticker, ArrayList<Long> tokens, String indexName) {
        ticker.setOnConnectedListener(() -> {
            // HPC meltdown synergy => single shot subscription => no chunk => sub-10ms aggregator
            logger.info("[onConnect] => totalTokens={}, subscribing now => index={}", tokens.size(), indexName);
            ticker.subscribe(tokens);
            ticker.setMode(tokens, KiteTicker.modeFull);
        });

        ticker.setOnDisconnectedListener(() -> {
            // HPC meltdown synergy => immediate or short-latency reconnect approach => sub-10ms if feasible
            logger.warn("Ticker disconnected => index={} => scheduling recon attempt now", indexName);
            scheduleReconnect(indexName, initialReconnectDelayMillis);
        });

        ticker.setOnErrorListener(new OnError() {
            @Override
            public void onError(Exception exception) {
                logger.error("KiteTicker exception => index={}, e=", indexName, exception);
            }
            @Override
            public void onError(String error) {
                logger.error("KiteTicker error => index={}, error={}", indexName, error);
            }
            @Override
            public void onError(KiteException kiteException) {
                logger.error("KiteTicker kiteException => index={}, e=", indexName, kiteException);
            }
        });

        ticker.setOnTickerArrivalListener(new OnTicks() {
            @Override
            public void onTicks(ArrayList<Tick> ticks) {
                long now = System.nanoTime();
                for(Tick tk : ticks) {
                    long token = tk.getInstrumentToken();
                    double ltp = tk.getLastTradedPrice();
                    // HPC meltdown synergy => ring buffer produce => O(1)
                    producer.publishTick(token, ltp, now, now);
                }
            }
        });
    }

    /**
     * HPC meltdown synergy => short-latency recon => sub-10ms approach => might do partial backoff if repeated fails
     */
    private void scheduleReconnect(String indexName, long delayMillis) {
        new Thread(() -> {
            try {
                // HPC meltdown synergy => short sleep => e.g. 10ms
                Thread.sleep(delayMillis);
                logger.info("Reconnecting => index={} => after {} ms", indexName, delayMillis);
                startWebSocketsForIndex(indexName);
            } catch(InterruptedException e) {
                logger.warn("Reconnect thread interrupted => no further reconnect => index={}", indexName, e);
            }
        }, "WebSocketReconnect").start();
    }

    /**
     * HPC meltdown synergy => how many websockets are active => for mesmerizing dashboard
     */
    public int getActiveWebSocketCount() {
        return tickers.size();
    }

    /**
     * HPC meltdown synergy => which indexes are currently subscribed => for mesmerizing dashboard
     */
    public List<String> getActiveIndexes() {
        return new ArrayList<>(activeIndexes);
    }
}
