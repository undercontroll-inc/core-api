package com.undercontroll.infrastructure.gateway;

final class MarketViewSql {

    static final String PARAM_BUCKET = "bucket";
    static final String PARAM_LIMIT = "limit";
    static final String PARAM_DOMAINS = "domains";
    static final String PARAM_MIN_PRICES = "minPrices";
    static final String PARAM_MIN_PAIRED = "minPaired";

    private static final String BRAND_COLUMNS = """
            brand_slug, bucket_key, brand_name, product_count, domain_count, avg_score, best_rank,
            avg_price_median, price_floor, price_ceiling, avg_price_delta_pct, avg_discount_pct, rising_count
            """;

    private static final String CATEGORY_COLUMNS = """
            domain_id, bucket_key, category_name_sample AS category_name, product_count, brand_count, avg_score,
            max_score, avg_price_median, price_floor, price_ceiling, avg_valid_price_count, avg_price_delta_pct,
            avg_discount_pct, rising_count, falling_count, high_confidence_count
            """;

    private static final String SAMPLE_COLUMNS = """
            available_offer_count, observed_offer_count, valid_price_count, sample_complete, observed_seller_count
            """;

    private static final String PRODUCT_COLUMNS = """
            bucket_key, source, external_type, external_id, title, canonical_title, rank, rank_band,
            rank_delta, trajectory_label, score, priority_label, confidence, domain_id, category_id,
            category_name, brand, brand_slug, model, product_key, voltage, power_watts, capacity_value,
            capacity_unit, energy_efficiency, price, price_min, price_median, price_max, price_delta_pct,
            discount_pct,
            """ + SAMPLE_COLUMNS + ", relevance, domain_reason, permalink ";

    private static final String RISING_COLUMNS = """
            bucket_key, external_type, external_id, title, brand, brand_slug, model, product_key,
            domain_id, category_id, category_name, rank, rank_band, rank_delta, trajectory_label,
            score, priority_label, confidence, price_median, price_min, price_max, price_delta_pct,
            discount_pct,
            """ + SAMPLE_COLUMNS + ", capacity_value, capacity_unit, power_watts, voltage, permalink ";

    private static final String MOVEMENT_COLUMNS = """
            domain_id, bucket_key, previous_bucket_key, consecutive, paired_count, current_product_count,
            previous_product_count, current_cohort_median, previous_cohort_median, price_index_delta_pct
            """;

    private static final String FROM_BRAND_SUMMARY = " FROM vw_market_brand_summary WHERE bucket_key = :bucket ";
    private static final String FROM_CATEGORY_SUMMARY = " FROM vw_market_category_summary WHERE bucket_key = :bucket ";
    private static final String FROM_PRODUCT_CURRENT = " FROM vw_market_product_current ";
    private static final String FROM_RELIABLE_MOVEMENT = """
             FROM vw_market_price_movement
            WHERE bucket_key = :bucket
              AND consecutive = TRUE
              AND previous_bucket_key IS NOT NULL
              AND price_index_delta_pct IS NOT NULL
              AND paired_count >= :minPaired
            """;

    static final String CURRENT_BUCKET = "SELECT bucket_key FROM vw_market_published_snapshot";
    static final String COUNT_PRODUCTS = "SELECT COUNT(*) FROM vw_market_product_current";
    static final String COUNT_BRANDS =
            "SELECT COUNT(DISTINCT brand_slug) FROM vw_market_product_current WHERE brand_slug IS NOT NULL";

    static final String TOP_BRANDS = "SELECT " + BRAND_COLUMNS + FROM_BRAND_SUMMARY
            + "ORDER BY product_count DESC, avg_score DESC LIMIT :limit";
    static final String BRAND_MOMENTUM = "SELECT " + BRAND_COLUMNS + FROM_BRAND_SUMMARY
            + "ORDER BY rising_count DESC, product_count DESC";

    static final String TOP_CATEGORIES = "SELECT " + CATEGORY_COLUMNS + FROM_CATEGORY_SUMMARY
            + "ORDER BY product_count DESC, avg_score DESC LIMIT :limit";
    static final String CATEGORY_SUMMARY = "SELECT " + CATEGORY_COLUMNS + FROM_CATEGORY_SUMMARY;
    static final String UNCOVERED_CATEGORIES = "SELECT " + CATEGORY_COLUMNS + FROM_CATEGORY_SUMMARY
            + "AND rising_count > 0 AND domain_id NOT IN (:domains) ORDER BY avg_score DESC";

    static final String ALL_CURRENT_PRODUCTS = "SELECT " + PRODUCT_COLUMNS + FROM_PRODUCT_CURRENT;
    static final String PRICE_DISPERSION = "SELECT " + PRODUCT_COLUMNS + FROM_PRODUCT_CURRENT
            + "WHERE external_type = 'PRODUCT' AND valid_price_count >= :minPrices"
            + " AND price_min IS NOT NULL AND price_max IS NOT NULL";

    static final String PRICE_MOVEMENTS = "SELECT " + MOVEMENT_COLUMNS + FROM_RELIABLE_MOVEMENT
            + "ORDER BY price_index_delta_pct DESC, domain_id";
    static final String STOCK_OPPORTUNITIES = "SELECT " + MOVEMENT_COLUMNS + FROM_RELIABLE_MOVEMENT
            + "AND price_index_delta_pct < 0 ORDER BY price_index_delta_pct ASC, domain_id";

    static final String RISING_PRODUCTS = "SELECT " + RISING_COLUMNS
            + " FROM vw_market_rising_products WHERE confidence = 'HIGH' ORDER BY score DESC";

    private MarketViewSql() {
    }
}
