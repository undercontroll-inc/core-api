package com.undercontroll.application.port;

public interface MetricsPort {

    void incrementLoginFailed();

    void incrementLoginSuccess();

    void incrementGoogleLoginFailed();

    void incrementGoogleLoginSuccess();

    void incrementAccountCreated();

    void incrementAccountCreationFailed();

    void incrementOrderCreated();

    void incrementOrderCompleted();

    void incrementOrderCancelled();

    void incrementOrderUpdateFailed();

    void recordOrderProcessingTime(long startTimeMillis);

    void incrementInsufficientStock(String componentName);

    void incrementComponentCreated();

    void incrementComponentUpdated();

    void incrementStockDecreased();

    void incrementDemandCreated();

    void incrementDemandRemoved();

    void incrementEmailSent();

    void incrementEmailFailed();

    void incrementAnnouncementCreated();

    void incrementAnnouncementEmailsSent(int count);

    void incrementCacheHit(String cacheName);

    void incrementCacheMiss(String cacheName);

    void incrementCacheEviction(String cacheName);
}
