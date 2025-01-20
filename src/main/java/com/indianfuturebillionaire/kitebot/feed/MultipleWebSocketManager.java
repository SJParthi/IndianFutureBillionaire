package com.indianfuturebillionaire.kitebot.feed;

import com.indianfuturebillionaire.kitebot.disruptor.TickEventProducer;
import com.indianfuturebillionaire.kitebot.model.NseCsvRecord;
import com.indianfuturebillionaire.kitebot.service.IndexCsvDownloadService;
import com.indianfuturebillionaire.kitebot.service.ZerodhaInstrumentsService;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.ticker.KiteTicker;
import com.zerodhatech.ticker.OnConnect;
import com.zerodhatech.ticker.OnDisconnect;
import com.zerodhatech.ticker.OnTicks;
import com.zerodhatech.models.Tick;
import com.zerodhatech.ticker.OnError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/***********************************************************************
 * HPC meltdown synergy => chunked websockets for NIFTY indexes
 * (NIFTY50, NIFTY100, NIFTY500, etc.).
 * We store references to each KiteTicker for aggregator meltdown synergy.
 * We also track "activeIndexes" so the dashboard can see which indexes
 * are currently subscribed.
 ***********************************************************************/
@Service
public class MultipleWebSocketManager {

    private static final Logger logger = LoggerFactory.getLogger(MultipleWebSocketManager.class);

    private final TickEventProducer producer;
    private final KiteConnect kiteConnect;
    // store references to each ticker => so we can stop them if meltdown
    private final List<KiteTicker> tickers = new ArrayList<>();
    private final AtomicBoolean started = new AtomicBoolean(false);

    // HPC => track which indexes are active
    private final Set<String> activeIndexes = new HashSet<>();

    private final IndexCsvDownloadService indexCsvDownloadService;

    public MultipleWebSocketManager(TickEventProducer p, KiteConnect kc,
                                    IndexCsvDownloadService indexCsvDownloadService) {
        this.producer = p;
        this.kiteConnect = kc;
        this.indexCsvDownloadService = indexCsvDownloadService;
    }

    /***********************************************************************
     * HPC meltdown synergy => start websockets for a given index
     * with chunking if needed.
     ***********************************************************************/
    public synchronized void startWebSocketsForIndex(String indexName) {
        logger.info("startWebSocketsForIndex => index={}, startedPreviously={}", indexName, started.get());
        if(started.get()) {
            stopAllWebSockets();
        }
        try {
            // HPC => in real usage, fetch tokens from an index or CSV
            List<Long> tokens = fetchTokensForIndex(indexName);
            if(tokens.isEmpty()) {
                logger.warn("No tokens => skipping start for index={}", indexName);
                return;
            }
            // chunk tokens => 200 each
            int chunkSize = 200;
            List<List<Long>> chunked = chunkTokens(tokens, chunkSize);
            for(List<Long> chunk : chunked) {
                ArrayList<Long> arrChunk = new ArrayList<>(chunk);
                KiteTicker ticker = new KiteTicker(kiteConnect.getAccessToken(), kiteConnect.getApiKey());
                registerHandlers(ticker, arrChunk);
                tickers.add(ticker);
                ticker.connect();
            }
            started.set(true);
            activeIndexes.clear();
            activeIndexes.add(indexName);
            logger.info("All websockets => index={}, totalTickers={}, totalTokens={}",
                    indexName, tickers.size(), tokens.size());
        } catch(Exception e) {
            logger.error("startWebSocketsForIndex => error => ", e);
        }
    }

    /***********************************************************************
     * HPC meltdown synergy => we can unsub or fully stop if meltdown triggers
     ***********************************************************************/
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
        logger.info("All websockets disconnected");
    }

    private List<Long> fetchTokensForIndex(String indexName) throws Exception {

        // 1) download CSV lines => returns List<NseCsvRecord>
        List<NseCsvRecord> records = indexCsvDownloadService.downloadIndexCsv(indexName);

        // 2) map them to tokens => returns List<Long>
        List<Long> tokens = indexCsvDownloadService.mapRecordsToTokens(records);

        logger.info("fetchTokensForIndex => index={}, totalTokens={}", indexName, tokens.size());
        return tokens;
    }

    private List<List<Long>> chunkTokens(List<Long> all, int chunkSize) {
        List<List<Long>> out = new ArrayList<>();
        for(int i=0; i<all.size(); i+=chunkSize) {
            int end = Math.min(i+chunkSize, all.size());
            out.add(all.subList(i, end));
        }
        return out;
    }

    private void registerHandlers(KiteTicker ticker, ArrayList<Long> tokenChunk) {
        ticker.setOnConnectedListener(new OnConnect() {
            @Override
            public void onConnected() {
                logger.info("[onConnect] => chunkSize={}, subscribing now", tokenChunk.size());
                ticker.subscribe(tokenChunk);
                ticker.setMode( tokenChunk,KiteTicker.modeFull );
            }
        });

        ticker.setOnDisconnectedListener(new OnDisconnect() {
            @Override
            public void onDisconnected() {
                System.out.println("Ticker disconnected");
            }
        });

        ticker.setOnErrorListener(new OnError() {
            @Override
            public void onError(Exception exception) {
                exception.printStackTrace();
            }
            /**
             * @param error
             */
            @Override
            public void onError(String error) {

            }
            @Override
            public void onError(KiteException kiteException) {
                kiteException.printStackTrace();
            }
        });


        ticker.setOnTickerArrivalListener(new OnTicks() {
            @Override
            public void onTicks(ArrayList<Tick> ticks) {
                long now = System.nanoTime();
                for(Tick tk : ticks) {
                    long token = tk.getInstrumentToken();
                    double ltp = tk.getLastTradedPrice();
                    // HPC meltdown aggregator => publish to ring buffer
                    producer.publishTick(token, ltp, now, now);
                }
            }
        });
        // HPC meltdown synergy => onError, onDisconnected if meltdown
    }

    /***********************************************************************
     * HPC => let the dashboard see how many websockets are active
     ***********************************************************************/
    public int getActiveWebSocketCount() {
        return tickers.size();
    }

    /***********************************************************************
     * HPC => which indexes are currently subscribed
     ***********************************************************************/
    public List<String> getActiveIndexes() {
        return new ArrayList<>(activeIndexes);
    }
}
