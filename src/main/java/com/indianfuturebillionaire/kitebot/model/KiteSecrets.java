package com.indianfuturebillionaire.kitebot.model;

public class KiteSecrets {
    private String apiKey;
    private String apiSecret;
    private String publicToken;
    private String userId;
    private String notificationEmail;
    private String notificationPhone;

    private String accessToken;

    public KiteSecrets() {}

    public KiteSecrets(String ak, String as, String at, String pt,
                       String uid, String email, String phone) {
        this.apiKey=ak; this.apiSecret=as; this.accessToken=at; this.publicToken=pt;
        this.userId=uid; this.notificationEmail=email; this.notificationPhone=phone;
    }

    // getters & setters ...

    public String getApiKey() {
        return apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getPublicToken() {
        return publicToken;
    }

    public String getUserId() {
        return userId;
    }

    public String getNotificationEmail() {
        return notificationEmail;
    }

    public String getNotificationPhone() {
        return notificationPhone;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setPublicToken(String publicToken) {
        this.publicToken = publicToken;
    }

}
