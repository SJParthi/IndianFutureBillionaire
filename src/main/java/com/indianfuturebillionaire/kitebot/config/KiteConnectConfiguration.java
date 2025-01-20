package com.indianfuturebillionaire.kitebot.config;

import com.indianfuturebillionaire.kitebot.model.KiteSecrets;

import com.zerodhatech.kiteconnect.KiteConnect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KiteConnectConfiguration {

    @Bean
    public KiteConnect kiteConnect(KiteSecrets secrets) {
        KiteConnect kc = new KiteConnect(secrets.getApiKey());
        kc.setUserId(secrets.getUserId());
        kc.setAccessToken(secrets.getAccessToken());
        kc.setPublicToken(secrets.getPublicToken());
        return kc;
    }
}
