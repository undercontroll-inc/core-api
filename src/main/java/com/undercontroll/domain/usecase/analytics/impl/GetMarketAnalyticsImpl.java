package com.undercontroll.domain.usecase.analytics.impl;

import com.undercontroll.domain.gateway.MarketViewGateway;
import com.undercontroll.domain.usecase.analytics.GetMarketAnalyticsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetMarketAnalyticsImpl implements GetMarketAnalyticsPort {

    private static final int TOP_N = 5;

    private final MarketViewGateway marketViewGateway;

    @Override
    @Cacheable(value = "marketAnalytics", key = "'current'")
    public Output execute() {
        return marketViewGateway.findCurrentBucketKey()
                .map(this::build)
                .orElseGet(Output::empty);
    }

    private Output build(String bucketKey) {
        List<BrandHighlight> brands = marketViewGateway.findTopBrands(bucketKey, TOP_N)
                .stream()
                .map(row -> new BrandHighlight(
                        row.brandName(),
                        row.brandSlug(),
                        row.productCount() == null ? 0L : row.productCount(),
                        row.bestRank(),
                        row.avgScore()
                ))
                .toList();
        List<CategoryHighlight> categories = marketViewGateway.findTopCategories(bucketKey, TOP_N)
                .stream()
                .map(row -> new CategoryHighlight(
                        row.domainId(),
                        row.categoryName(),
                        row.productCount() == null ? 0L : row.productCount(),
                        row.avgScore(),
                        row.risingCount()
                ))
                .toList();
        return new Output(
                bucketKey,
                marketViewGateway.countCurrentProducts(),
                marketViewGateway.countDistinctBrands(),
                brands,
                categories
        );
    }
}
