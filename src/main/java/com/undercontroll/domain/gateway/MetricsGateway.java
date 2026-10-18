package com.undercontroll.domain.gateway;

public interface MetricsGateway {

    void increment(MetricName metric);

    void recordOrderProcessingTime(long startTimeMillis);
}
