package com.indianfuturebillionaire.kitebot.service;

import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.Instrument;
import com.zerodhatech.kiteconnect.KiteConnect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/***********************************************************************
 * HPC meltdown aggregator => We no longer auto-load at startup.
 * Instead, we have methods to load instruments from:
 *   1) The live KiteConnect.getInstruments() API
 *   2) A local CSV file (like instruments.csv)
 *
 * This class doesn't do anything until you call one of those methods.
 ***********************************************************************/
@Service
public class ZerodhaInstrumentsService {

    private static final Logger logger = LoggerFactory.getLogger(ZerodhaInstrumentsService.class);

    // "INFY" => 738561, "RELIANCE" => 738561, etc.
    private final Map<String, Long> symbolToTokenMap = new HashMap<>();

//    /**
//     * HPC aggregator => If you want the real instruments from an API call:
//     * kiteConnect.getInstruments() returns List<Instrument> in code.
//     * Then we parse each => store in symbolToTokenMap.
//     */
//    public void loadInstrumentsFromKiteConnect(KiteConnect kiteConnect) {
//        try {
//            logger.info("Loading instruments from live KiteConnect.getInstruments() ...");
//            List<Instrument> list = kiteConnect.getInstruments(); // can throw exception if session not valid
//
//            symbolToTokenMap.clear();
//            for(Instrument inst : list) {
//                // e.g. inst.tradingsymbol = "INFY", inst.instrument_token = 738561
//                String symbol = inst.tradingsymbol != null ? inst.tradingsymbol.trim().toUpperCase() : "";
//                long token = inst.instrument_token;
//                if(!symbol.isEmpty()) {
//                    symbolToTokenMap.put(symbol, token);
//                }
//            }
//            logger.info("Loaded {} instruments from kiteConnect.getInstruments()", symbolToTokenMap.size());
//        } catch(KiteException ke){
//            logger.error("Error fetching instruments from KiteConnect in Kite Exception => ", ke);
//        }catch(Exception e) {
//            logger.error("Error fetching instruments from KiteConnect => ", e);
//        }
//    }

    /**
     * HPC aggregator => If you want to parse local 'instruments.csv' instead:
     *  e.g. columns: instrument_token, exchange_token, tradingsymbol, ...
     */
    public void loadInstrumentsFromLocalCsv(String resourcePath) {
        try {
            logger.info("Loading instruments from local CSV => {}", resourcePath);
            InputStream is = getClass().getResourceAsStream(resourcePath);
            if(is==null) {
                logger.error("Local instruments CSV not found => {}", resourcePath);
                return;
            }
            parseLocalCsv(is);
        } catch(Exception e) {
            logger.error("Error loading local instruments CSV => ", e);
        }
    }

    private void parseLocalCsv(InputStream is) throws Exception {
        symbolToTokenMap.clear();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            // skip header
            br.readLine();
            String line;
            while((line=br.readLine()) != null) {
                if(line.trim().isEmpty()) continue;
                String[] parts = line.split(",", -1);
                if(parts.length<3) continue;
                // parts[0]=instrument_token, parts[2]=tradingsymbol
                long token=0;
                try {
                    token = Long.parseLong(parts[0]);
                } catch(Exception ex) {
                    continue;
                }
                String symbol = parts[2].trim().toUpperCase();
                if(!symbol.isEmpty()) {
                    symbolToTokenMap.put(symbol, token);
                }
            }
        }
        logger.info("Parsed local CSV => total instruments={}", symbolToTokenMap.size());
    }

    /**
     * HPC aggregator => get the real token for e.g. "INFY"
     */
    public long getTokenForSymbol(String symbol) {
        if(symbol==null) return 0L;
        String s = symbol.trim().toUpperCase();
        return symbolToTokenMap.getOrDefault(s, 0L);
    }
}
