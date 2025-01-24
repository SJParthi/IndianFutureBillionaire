package com.indianfuturebillionaire.kitebot.controller;

import com.indianfuturebillionaire.kitebot.config.AWSSecretsConfig;
import com.indianfuturebillionaire.kitebot.feed.WebSocketManager;
import com.indianfuturebillionaire.kitebot.model.KiteSecrets;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.util.Map;

/****************************************************************
 * Zerodha callback after daily login => generate session,
 * update AWS secrets, start multi websockets for aggregator.
 ****************************************************************/
@RestController
@RequestMapping("/kite")
public class KiteLoginController {

    private static final Logger logger = LoggerFactory.getLogger(KiteLoginController.class);

    private final KiteConnect kiteConnect;
    private final KiteSecrets kiteSecrets;
    private final AWSSecretsConfig awsSecretsConfig;
    private final SecretsManagerClient smClient;
    private final WebSocketManager wsManager;

    public KiteLoginController(KiteConnect kc,
                               KiteSecrets ks,
                               AWSSecretsConfig awsCfg,
                               SecretsManagerClient sm,
                               WebSocketManager wsm) {
        this.kiteConnect = kc;
        this.kiteSecrets = ks;
        this.awsSecretsConfig = awsCfg;
        this.smClient = sm;
        this.wsManager = wsm;
    }

    @GetMapping("/callback")
    public String handleLoginCallback(@RequestParam Map<String,String> params) {
        String requestToken = params.get("request_token");
        String status = params.get("status");
        if (requestToken==null || !"success".equalsIgnoreCase(status)) {
            logger.error("Invalid callback from Zerodha => missing token or status");
            return "Error: invalid callback from Zerodha.";
        }

        try {
            User user = kiteConnect.generateSession(requestToken, kiteSecrets.getApiSecret());
            kiteSecrets.setAccessToken(user.accessToken);
            kiteSecrets.setPublicToken(user.publicToken);
            kiteConnect.setAccessToken(user.accessToken);
            kiteConnect.setPublicToken(user.publicToken);
            awsSecretsConfig.updateKiteSecretsInAWS(smClient, kiteSecrets);

            // start websockets for a default index or multiple indexes
            wsManager.startWebSocketsForIndex("NIFTY 500");

            return "Login success! aggregator feed started for NIFTY 500. You can close this page.";
        } catch (KiteException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            logger.error("Error generating session or starting feed", e);
            return "Failed to generate session => " + e.getMessage();
        }
    }
}
