CREATE TABLE vw_market_brand_summary (
    brand_slug VARCHAR(80), bucket_key VARCHAR(7), brand_name VARCHAR(120), product_count BIGINT, domain_count INT,
    avg_score NUMERIC(10, 2), best_rank INT, avg_price_median NUMERIC(12, 2), price_floor NUMERIC(12, 2),
    price_ceiling NUMERIC(12, 2), avg_price_delta_pct NUMERIC(10, 2), avg_discount_pct NUMERIC(10, 2), rising_count BIGINT
);
CREATE TABLE vw_market_category_summary (
    domain_id VARCHAR(80), bucket_key VARCHAR(7), category_name_sample VARCHAR(120), product_count BIGINT,
    brand_count BIGINT, avg_score NUMERIC(10, 2), max_score NUMERIC(10, 2), avg_price_median NUMERIC(12, 2),
    price_floor NUMERIC(12, 2), price_ceiling NUMERIC(12, 2), avg_offer_count NUMERIC(10, 2),
    avg_price_delta_pct NUMERIC(10, 2), avg_discount_pct NUMERIC(10, 2), rising_count BIGINT, falling_count BIGINT,
    high_confidence_count BIGINT
);
CREATE TABLE vw_market_price_movement (
    domain_id VARCHAR(80), bucket_key VARCHAR(7), previous_bucket_key VARCHAR(7), product_count BIGINT,
    offer_count BIGINT, seller_count BIGINT, avg_price_median NUMERIC(12, 2), previous_price_median NUMERIC(12, 2),
    price_delta_pct NUMERIC(10, 2), offer_delta_pct NUMERIC(10, 2)
);
CREATE TABLE vw_market_product_current (
    bucket_key VARCHAR(7), source VARCHAR(20), external_type VARCHAR(20), external_id VARCHAR(40), title VARCHAR(200),
    canonical_title VARCHAR(200), rank INT, rank_band VARCHAR(20), rank_delta INT, trajectory_label VARCHAR(20),
    score NUMERIC(10, 2), priority_label VARCHAR(20), confidence VARCHAR(10), domain_id VARCHAR(80),
    category_id VARCHAR(40), category_name VARCHAR(120), brand VARCHAR(80), brand_slug VARCHAR(80), model VARCHAR(80),
    product_key VARCHAR(160), voltage VARCHAR(10), power_watts INT, capacity_value NUMERIC(10, 2),
    capacity_unit VARCHAR(10), energy_efficiency VARCHAR(5), price NUMERIC(12, 2), price_min NUMERIC(12, 2),
    price_median NUMERIC(12, 2), price_max NUMERIC(12, 2), price_delta_pct NUMERIC(10, 2), discount_pct NUMERIC(10, 2),
    offer_count INT, seller_count INT, relevance NUMERIC(10, 4), domain_reason VARCHAR(80), permalink VARCHAR(300)
);
CREATE TABLE vw_market_rising_products (
    bucket_key VARCHAR(7), external_type VARCHAR(20), external_id VARCHAR(40), title VARCHAR(200), brand VARCHAR(80),
    brand_slug VARCHAR(80), model VARCHAR(80), product_key VARCHAR(160), domain_id VARCHAR(80), category_id VARCHAR(40),
    category_name VARCHAR(120), rank INT, rank_band VARCHAR(20), rank_delta INT, trajectory_label VARCHAR(20),
    score NUMERIC(10, 2), priority_label VARCHAR(20), confidence VARCHAR(10), price_median NUMERIC(12, 2),
    price_min NUMERIC(12, 2), price_max NUMERIC(12, 2), price_delta_pct NUMERIC(10, 2), discount_pct NUMERIC(10, 2),
    offer_count INT, seller_count INT, capacity_value NUMERIC(10, 2), capacity_unit VARCHAR(10), power_watts INT,
    voltage VARCHAR(10), permalink VARCHAR(300)
);

INSERT INTO vw_market_brand_summary VALUES
    ('electrolux', '2026-08', 'Electrolux', 10, 2, 80.50, 1, 599.90, 400.00, 800.00, 2.50, 5.00, 3),
    ('mondial', '2026-08', 'Mondial', 4, 1, 70.00, 3, 199.90, 150.00, 250.00, NULL, NULL, 5);
INSERT INTO vw_market_category_summary VALUES
    ('MLB-MICROWAVES', '2026-08', 'Micro-ondas', 12, 4, 70.00, 90.00, 599.00, 400.00, 800.00, 6.00, 1.00, 4.00, 2, 1, 2),
    ('MLB-AIR_FRYERS', '2026-08', 'Air fryer', 8, 3, 75.00, 88.00, 299.00, 200.00, 400.00, 5.00, -3.00, 2.00, 1, 0, 1);
INSERT INTO vw_market_price_movement VALUES
    ('MLB-MICROWAVES', '2026-08', '2026-07', 4, 10, 3, 599.00, 540.00, 10.90, 2.00),
    ('MLB-AIR_FRYERS', '2026-08', '2026-07', 3, 12, 4, 299.00, 320.00, -6.50, 8.00);
INSERT INTO vw_market_product_current VALUES
    ('2026-08', 'ML', 'PRODUCT', 'MLB1', 'Micro-ondas 20L', 'micro-ondas 20l', 1, 'TOP10', 2, 'RISING', 91.20, 'HIGH',
     'HIGH', 'MLB-MICROWAVES', 'MLB1', 'Micro-ondas', 'Electrolux', 'electrolux', 'ME20', 'electrolux:me20', '220V',
     1200, 20.00, 'L', 'A', 599.90, 549.90, 579.90, 649.90, 3.20, 7.50, 9, 4, 0.9876, 'catalog', 'https://ml/1');
INSERT INTO vw_market_rising_products VALUES
    ('2026-08', 'PRODUCT', 'MLB1', 'Micro-ondas 20L', 'Electrolux', 'electrolux', 'ME20', 'electrolux:me20',
     'MLB-MICROWAVES', 'MLB1', 'Micro-ondas', 1, 'TOP10', 2, 'RISING', 91.20, 'HIGH', 'HIGH', 579.90, 549.90, 649.90,
     3.20, 7.50, 9, 4, 20.00, 'L', 1200, '220V', 'https://ml/1');
