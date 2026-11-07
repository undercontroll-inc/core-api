package com.undercontroll.domain.gateway;

import com.undercontroll.domain.model.market.MarketBrandSummary;
import com.undercontroll.domain.model.market.MarketCategorySummary;
import com.undercontroll.domain.model.market.MarketPriceMovement;
import com.undercontroll.domain.model.market.MarketProductCurrent;
import com.undercontroll.domain.model.market.MarketRisingProduct;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MarketViewGateway {

    Optional<String> findCurrentBucketKey();

    long countCurrentProducts();

    long countDistinctBrands();

    List<MarketBrandSummary> findTopBrands(String bucketKey, int limit);

    List<MarketCategorySummary> findTopCategories(String bucketKey, int limit);

    List<MarketProductCurrent> findAllCurrentProducts();

    List<MarketPriceMovement> findPriceMovements(String bucketKey);

    List<MarketPriceMovement> findStockOpportunities(String bucketKey);

    List<MarketRisingProduct> findRisingProductsHighConfidence();

    List<MarketBrandSummary> findBrandMomentum(String bucketKey);

    List<MarketCategorySummary> findCategorySummary(String bucketKey);

    List<MarketProductCurrent> findPriceDispersion();

    List<MarketCategorySummary> findUncoveredCategories(String bucketKey, Collection<String> clientDomainIds);
}
