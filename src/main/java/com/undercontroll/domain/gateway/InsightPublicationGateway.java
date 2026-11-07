package com.undercontroll.domain.gateway;

import com.undercontroll.domain.model.insight.MarketInsightPublication;

import java.util.Optional;

public interface InsightPublicationGateway {

    Optional<MarketInsightPublication> findLatest();
}
