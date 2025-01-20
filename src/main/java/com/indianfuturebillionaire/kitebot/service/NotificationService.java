package com.indianfuturebillionaire.kitebot.service;

import com.indianfuturebillionaire.kitebot.config.AwsModeConfig;
import com.indianfuturebillionaire.kitebot.model.KiteSecrets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private static final Region AWS_REGION=Region.AP_SOUTH_1;
    private static final String FROM_EMAIL="verified@yourdomain.com";

    private final KiteSecrets secrets;
    private final SesClient sesClient;
    private final SnsClient snsClient;

    public NotificationService(KiteSecrets s) {
        this.secrets = s;
        this.sesClient = SesClient.builder()
                .region(AWS_REGION)
                .credentialsProvider(AwsModeConfig.buildCredentialsProvider())
                .build();
        this.snsClient = SnsClient.builder()
                .region(AWS_REGION)
                .credentialsProvider(AwsModeConfig.buildCredentialsProvider())
                .build();
    }

    public void sendEmail(String subject, String body) {
        try {
            String toEmail = secrets.getNotificationEmail();
            SendEmailRequest req = SendEmailRequest.builder()
                    .source(FROM_EMAIL)
                    .destination(Destination.builder().toAddresses(toEmail).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).build())
                            .body(Body.builder()
                                    .text(Content.builder().data(body).build())
                                    .build())
                            .build())
                    .build();
            sesClient.sendEmail(req);
            logger.info("Email sent => to={}, subject={}", toEmail, subject);
        } catch(Exception e) {
            logger.error("SES email error", e);
        }
    }

    public void sendSms(String msg) {
        try {
            String phone = secrets.getNotificationPhone();
            PublishRequest pubReq = PublishRequest.builder()
                    .phoneNumber(phone)
                    .message(msg)
                    .build();
            snsClient.publish(pubReq);
            logger.info("SMS => phone={}", phone);
        } catch(Exception e) {
            logger.error("SNS sms error", e);
        }
    }
}
