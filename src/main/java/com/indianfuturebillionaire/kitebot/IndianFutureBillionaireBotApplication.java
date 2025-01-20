package com.indianfuturebillionaire.kitebot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/****************************************************************
 * Main Spring Boot entry point, ensuring we read application.yml
 * and auto-scan all components for aggregator, feed, etc.
 ****************************************************************/
@SpringBootApplication
public class IndianFutureBillionaireBotApplication {
    public static void main(String[] args) {
        SpringApplication.run(IndianFutureBillionaireBotApplication.class, args);
    }
}
