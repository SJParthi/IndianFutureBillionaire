package com.indianfuturebillionaire.kitebot.service;

import com.indianfuturebillionaire.kitebot.model.NseCsvRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/***********************************************************************
 * HPC meltdown aggregator => local CSV approach for e.g. NIFTY50, NIFTY100,
 * NIFTY500. We store them in src/main/resources/csv/ and parse them
 * from the classpath instead of fetching from the net.
 *
 * 1) Each index => "NIFTY50" mapped to "/csv/ind_nifty50list.csv"
 * 2) Parse lines => build NseCsvRecord
 * 3) mapRecordsToTokens(...) => placeholder => HPC aggregator usage
 ***********************************************************************/
@Service
public class IndexCsvDownloadService {

    private static final Logger logger = LoggerFactory.getLogger(IndexCsvDownloadService.class);

    // Map from "NIFTY50" => "classpath path to CSV"
    private static final Map<String, String> CSV_RESOURCE_PATHS = new HashMap<>();
    static {
        CSV_RESOURCE_PATHS.put("NIFTY50",         "/csv/ind_nifty50list.csv");
        CSV_RESOURCE_PATHS.put("NIFTY100",        "/csv/ind_nifty100list.csv");
        CSV_RESOURCE_PATHS.put("NIFTY500",        "/csv/ind_nifty500list.csv");
        CSV_RESOURCE_PATHS.put("NIFTYMIDCAP100",  "/csv/ind_niftymidcap100list.csv");
        CSV_RESOURCE_PATHS.put("NIFTYSMALLCAP100","/csv/ind_niftysmallcap100list.csv");
    }

    private final ZerodhaInstrumentsService zerodhaInstrumentsService;

    public IndexCsvDownloadService(ZerodhaInstrumentsService zerodhaInstrumentsService) {
        this.zerodhaInstrumentsService = zerodhaInstrumentsService;
    }

    /**
     * Fetches the CSV for the given indexName (e.g. "NIFTY50"),
     * parses lines into NseCsvRecord.
     * If indexName not found, defaults to "NIFTY500".
     */
    public List<NseCsvRecord> downloadIndexCsv(String indexName) throws Exception {

        // HPC => uppercase the index for the map
        String idxKey = indexName.toUpperCase();
        // fallback if not found
        String resourcePath = CSV_RESOURCE_PATHS.getOrDefault(idxKey, "/csv/ind_nifty500list.csv");

        logger.info("Loading local CSV => index={}, resourcePath={}", indexName, resourcePath);

        // We'll parse from the classpath resource.
        // If these files are in src/main/resources/csv,
        // you can load them with getResourceAsStream(resourcePath).
        List<NseCsvRecord> recs = new ArrayList<>();
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if(is == null) {
                logger.error("Resource not found => " + resourcePath);
                return recs; // empty
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                boolean firstLine = true;
                String line;
                while((line=br.readLine()) != null) {
                    if(firstLine) {
                        firstLine=false; // skip header
                        continue;
                    }
                    String[] parts = line.split(",", -1);
                    if(parts.length<5) continue;
                    // e.g. [0]: Company Name, [1]: Industry, [2]: Symbol, [3]: Series, [4]: ISIN
                    recs.add(new NseCsvRecord(parts[0], parts[1], parts[2], parts[3], parts[4]));
                }
            }
        }
        logger.info("downloadIndexCsv => index={}, totalRecords={}", indexName, recs.size());
        return recs;
    }

    /**
     * HPC meltdown synergy => map CSV records to tokens.
     * Real usage => cross-reference Zerodha instrument token from
     * a local "instruments.csv" or from the official instruments dump.
     * We'll do a placeholder approach => token=100000 + i
     */
    public List<Long> mapRecordsToTokens(List<NseCsvRecord> records) {

        //Load All the NSE Kite Instruments
        zerodhaInstrumentsService.loadInstrumentsFromLocalCsv("/csv/instruments_nse.csv");

        List<Long> tokens = new ArrayList<>();
        for(NseCsvRecord r : records) {
            String symbol = r.getSymbol();
            if(symbol==null) continue;
            symbol = symbol.trim().toUpperCase();
            // HPC aggregator => real token from instrumentsService
            long realToken = zerodhaInstrumentsService.getTokenForSymbol(symbol);
            if(realToken==0L) {
                logger.warn("No real token for symbol={}, skip", symbol);
                continue;
            }
            tokens.add(realToken);
        }
        logger.info("mapRecordsToTokens => mapped {} tokens from {} records", tokens.size(), records.size());
        return tokens;
    }
}
