package com.indianfuturebillionaire.kitebot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;

public class AwsModeConfig {

    private static final Logger logger = LoggerFactory.getLogger(AwsModeConfig.class);

    private AwsModeConfig() {}

    public static AwsCredentialsProvider buildCredentialsProvider() {
        String env = System.getenv("ENV");
        if ("AWS".equalsIgnoreCase(env)) {
            logger.info("Using DefaultCredentialsProvider for AWS environment");
            return DefaultCredentialsProvider.create();
        } else if ("LOCAL".equalsIgnoreCase(env)) {
            logger.info("Using ProfileCredentialsProvider('default') for local dev environment");
            return ProfileCredentialsProvider.create("default");
        } else {
            logger.info("No ENV set => using DefaultCredentialsProvider chain");
            return DefaultCredentialsProvider.create();
        }
    }
}
