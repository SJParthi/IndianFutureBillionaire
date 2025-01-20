//package com.indianfuturebillionaire.kitebot;
//
//import java.io.*;
//import java.nio.charset.StandardCharsets;
//
///***********************************************************************
// * A standalone main class to filter the large instruments.csv
// * so that only NSE/EQ rows remain, writing them to instruments_nse.csv.
// *
// * Usage:
// *   1) Place instruments.csv somewhere accessible (in your project or a known path).
// *   2) Edit inputFile or pass as an argument.
// *   3) Run -> generates instruments_nse.csv in your working directory.
// *
// * No Spring or bean config: purely a main() you can run from IDE or CLI.
// ***********************************************************************/
//public class InstrumentFilterMain {
//
//    public static void main(String[] args) {
//        // You can adjust the input path and output path here,
//        // or parse args if you want more dynamic usage.
//        String inputFile = "src/main/resources/csv/instruments.csv";
//        String outputFile = "instruments_nse.csv"; // or specify "src/main/resources/csv/instruments_nse.csv"
//
//        filterNseEqInstruments(inputFile, outputFile);
//    }
//
//    /**
//     * Filter the large CSV for only rows with exchange=NSE and instrument_type=EQ.
//     */
//    public static void filterNseEqInstruments(String inputFilePath, String outputFilePath) {
//        System.out.println("Filtering instruments => " + inputFilePath + " => " + outputFilePath);
//
//        File inputFile = new File(inputFilePath);
//        if(!inputFile.exists()) {
//            System.err.println("Input file not found => " + inputFile.getAbsolutePath());
//            return;
//        }
//
//        try (
//                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8));
//                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFilePath), StandardCharsets.UTF_8))
//        ) {
//            // read header
//            String header = br.readLine();
//            if(header != null) {
//                bw.write(header);
//                bw.newLine();
//            }
//            int total=0, kept=0;
//            String line;
//            while((line=br.readLine())!=null) {
//                total++;
//                String[] parts = line.split(",", -1);
//                String instrumentType = parts[9].trim(); // "EQ", "CE", "PE", ...
//                String segment = parts[10].trim();
//                String exchange = parts[11].trim(); // "NSE", "BSE", etc.
//                if("EQ".equalsIgnoreCase(instrumentType) && "NSE".equalsIgnoreCase(segment) && "NSE".equalsIgnoreCase(exchange)) {
//                    bw.write(line);
//                    bw.newLine();
//                    kept++;
//                }
//            }
//            bw.flush();
//            System.out.println("Completed => total lines read=" + total + ", lines kept=" + kept);
//        } catch(Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
