package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.gateway.MarketViewGateway;
import com.undercontroll.domain.model.market.MarketBrandSummary;
import com.undercontroll.domain.model.market.MarketCategorySummary;
import com.undercontroll.domain.model.market.MarketPriceMovement;
import com.undercontroll.domain.model.market.MarketProductCurrent;
import com.undercontroll.domain.model.market.MarketRisingProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MarketViewGatewayImpl implements MarketViewGateway {

    private static final RowMapper<MarketBrandSummary> BRAND = new DataClassRowMapper<>(MarketBrandSummary.class);
    private static final RowMapper<MarketCategorySummary> CATEGORY = new DataClassRowMapper<>(MarketCategorySummary.class);
    private static final RowMapper<MarketPriceMovement> MOVEMENT = new DataClassRowMapper<>(MarketPriceMovement.class);
    private static final RowMapper<MarketProductCurrent> PRODUCT = new DataClassRowMapper<>(MarketProductCurrent.class);
    private static final RowMapper<MarketRisingProduct> RISING = new DataClassRowMapper<>(MarketRisingProduct.class);

    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public Optional<String> findCurrentBucketKey() {
        return Optional.ofNullable(safe(
                () -> jdbc.getJdbcTemplate().queryForObject(MarketViewSql.CURRENT_BUCKET, String.class),
                null));
    }

    @Override
    public Optional<String> findPreviousBucketKey(String currentBucketKey) {
        if (currentBucketKey == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(safe(
                () -> jdbc.queryForObject(MarketViewSql.PREVIOUS_BUCKET, bucketParams(currentBucketKey), String.class),
                null));
    }

    @Override
    public long countCurrentProducts() {
        return count(MarketViewSql.COUNT_PRODUCTS);
    }

    @Override
    public long countDistinctBrands() {
        return count(MarketViewSql.COUNT_BRANDS);
    }

    @Override
    public List<MarketBrandSummary> findTopBrands(String bucketKey, int limit) {
        return safeList(() -> jdbc.query(MarketViewSql.TOP_BRANDS, bucketAndLimit(bucketKey, limit), BRAND));
    }

    @Override
    public List<MarketCategorySummary> findTopCategories(String bucketKey, int limit) {
        return safeList(() -> jdbc.query(MarketViewSql.TOP_CATEGORIES, bucketAndLimit(bucketKey, limit), CATEGORY));
    }

    @Override
    public List<MarketProductCurrent> findAllCurrentProducts() {
        return safeList(() -> jdbc.getJdbcTemplate().query(MarketViewSql.ALL_CURRENT_PRODUCTS, PRODUCT));
    }

    @Override
    public List<MarketPriceMovement> findPriceMovements(String bucketKey) {
        return safeList(() -> jdbc.query(MarketViewSql.PRICE_MOVEMENTS, bucketParams(bucketKey), MOVEMENT));
    }

    @Override
    public List<MarketPriceMovement> findStockOpportunities(String bucketKey) {
        return safeList(() -> jdbc.query(MarketViewSql.STOCK_OPPORTUNITIES, bucketParams(bucketKey), MOVEMENT));
    }

    @Override
    public List<MarketRisingProduct> findRisingProductsHighConfidence() {
        return safeList(() -> jdbc.getJdbcTemplate().query(MarketViewSql.RISING_PRODUCTS, RISING));
    }

    @Override
    public List<MarketBrandSummary> findBrandMomentum(String bucketKey) {
        return safeList(() -> jdbc.query(MarketViewSql.BRAND_MOMENTUM, bucketParams(bucketKey), BRAND));
    }

    @Override
    public List<MarketCategorySummary> findCategorySummary(String bucketKey) {
        return safeList(() -> jdbc.query(MarketViewSql.CATEGORY_SUMMARY, bucketParams(bucketKey), CATEGORY));
    }

    @Override
    public List<MarketProductCurrent> findPriceDispersion() {
        return safeList(() -> jdbc.getJdbcTemplate().query(MarketViewSql.PRICE_DISPERSION, PRODUCT));
    }

    @Override
    public List<MarketCategorySummary> findUncoveredCategories(String bucketKey, Collection<String> clientDomainIds) {
        Collection<String> domains = (clientDomainIds == null || clientDomainIds.isEmpty())
                ? List.of("__none__")
                : clientDomainIds;
        MapSqlParameterSource params = bucketParams(bucketKey).addValue(MarketViewSql.PARAM_DOMAINS, domains);
        return safeList(() -> jdbc.query(MarketViewSql.UNCOVERED_CATEGORIES, params, CATEGORY));
    }

    private long count(String sql) {
        Long count = safe(() -> jdbc.getJdbcTemplate().queryForObject(sql, Long.class), 0L);
        return count == null ? 0L : count;
    }

    private static MapSqlParameterSource bucketParams(String bucketKey) {
        return new MapSqlParameterSource(MarketViewSql.PARAM_BUCKET, bucketKey);
    }

    private static MapSqlParameterSource bucketAndLimit(String bucketKey, int limit) {
        return bucketParams(bucketKey).addValue(MarketViewSql.PARAM_LIMIT, limit);
    }

    private <T> T safe(Supplier<T> query, T fallback) {
        try {
            return query.get();
        } catch (DataAccessException ex) {
            log.warn("Market view unavailable: {}", ex.getMessage());
            return fallback;
        }
    }

    private <T> List<T> safeList(Supplier<List<T>> query) {
        List<T> result = safe(query, Collections.emptyList());
        return result == null ? Collections.emptyList() : result;
    }
}
