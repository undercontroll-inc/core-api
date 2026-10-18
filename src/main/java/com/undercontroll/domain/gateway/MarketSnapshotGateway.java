package com.undercontroll.domain.gateway;

import com.undercontroll.domain.model.market.PublishedMarketSnapshot;

import java.util.Optional;

public interface MarketSnapshotGateway {

    Optional<PublishedMarketSnapshot> findPublished();
}
