package com.indianfuturebillionaire.kitebot.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indianfuturebillionaire.kitebot.model.KiteSecrets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.*;

@Configuration
public class AWSSecretsConfig {

    private static final Logger logger = LoggerFactory.getLogger(AWSSecretsConfig.class);

    private static final String SECRET_NAME = "Kite_Connect_Secret_Keys";
    private static final Region AWS_REGION  = Region.AP_SOUTH_1;

    @Bean
    public SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder()
                .region(AWS_REGION)
                .credentialsProvider(AwsModeConfig.buildCredentialsProvider())
                .build();
    }

    @Bean
    public KiteSecrets kiteSecrets(SecretsManagerClient smClient) throws Exception {
        GetSecretValueResponse resp = smClient.getSecretValue(
                GetSecretValueRequest.builder().secretId(SECRET_NAME).build()
        );
        String secretJson = resp.secretString();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(secretJson);

        String apiKey      = node.get("apiKey").asText();
        String apiSecret   = node.get("apiSecret").asText();
        String accessToken = node.get("accessToken").asText();
        String publicToken = node.get("publicToken").asText();
        String userId      = node.get("userId").asText();
        String email       = node.get("notificationEmail").asText();
        String phone       = node.get("notificationPhone").asText();

        logger.info("Loaded secrets => userId={}, email={}, phone={}, hasAccessToken={}",
                userId, email, phone, (accessToken!=null));

        return new KiteSecrets(apiKey, apiSecret, accessToken, publicToken, userId, email, phone);
    }

    public void updateKiteSecretsInAWS(SecretsManagerClient smClient, KiteSecrets secrets) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String updatedJson = mapper.writeValueAsString(secrets);

        PutSecretValueResponse putResp = smClient.putSecretValue(
                PutSecretValueRequest.builder()
                        .secretId(SECRET_NAME)
                        .secretString(updatedJson)
                        .build()
        );
        logger.info("AWS secrets updated => versionId={}", putResp.versionId());
    }
}
