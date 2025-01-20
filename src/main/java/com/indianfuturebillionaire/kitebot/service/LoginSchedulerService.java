package com.indianfuturebillionaire.kitebot.service;

import com.indianfuturebillionaire.kitebot.config.MarketHolidaysConfig;
import com.indianfuturebillionaire.kitebot.model.KiteSecrets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class LoginSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(LoginSchedulerService.class);

    private final KiteSecrets secrets;
    private final NotificationService notifier;
    private final MarketHolidaysConfig holidays;

    public LoginSchedulerService(KiteSecrets s, NotificationService n, MarketHolidaysConfig h) {
        this.secrets = s;
        this.notifier = n;
        this.holidays = h;
    }

    @Scheduled(cron="0 45 8 * * MON-FRI", zone="Asia/Kolkata")
    public void sendLoginReminder() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        if (holidays.isHoliday(today)) {
            logger.info("Holiday => skip login reminder for {}", today);
            return;
        }
        String loginUrl = "https://kite.zerodha.com/connect/login?api_key=" + secrets.getApiKey() + "&v=3";
        String subject = "[KiteBot] Daily Login => " + today;
        String body = "Please login => " + loginUrl + "\nAfter login => /kite/callback?status=success";

        notifier.sendEmail(subject, body);
        notifier.sendSms("KiteBot daily login => " + loginUrl);
        logger.info("Daily login link => date={}, url={}", today, loginUrl);
    }
}
