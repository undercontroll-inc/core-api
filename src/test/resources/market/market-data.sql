INSERT INTO market_published_snapshot (snapshot_id, run_id, bucket_key, status, quality_report, published_at) VALUES
    (1, 'run-old', '2026-07', 'SUCCESS', '{"verdict":"PASS"}' FORMAT JSON, TIMESTAMP WITH TIME ZONE '2026-08-01 03:00:00+00'),
    (2, 'run-pub', '2026-08', 'SUCCESS', '{"verdict":"PASS"}' FORMAT JSON, TIMESTAMP WITH TIME ZONE '2026-09-01 03:00:00+00');

INSERT INTO market_curated_product (run_id, bucket_key, source, external_type, external_id, title, canonical_title, rank,
    rank_band, rank_delta, trajectory_label, score, priority_label, confidence, domain_id, category_id, category_name,
    brand, brand_slug, model, product_key, voltage, power_watts, capacity_value, capacity_unit, energy_efficiency, price,
    price_min, price_median, price_max, price_delta_pct, discount_pct, available_offer_count, observed_offer_count,
    valid_price_count, sample_complete, observed_seller_count, relevance, domain_reason, permalink) VALUES
    ('run-pub', '2026-08', 'ML', 'PRODUCT', 'MLB1', 'Micro-ondas 20L', 'micro-ondas 20l', 1, 'TOP10', 2,
     'CRESCIMENTO', 91.20, 'PRIORIDADE_ALTA', 'HIGH', 'MLB-MICROWAVES', 'MLB1', 'Micro-ondas', 'Electrolux',
     'electrolux', 'ME20', 'electrolux:me20', '220V', 1200, 20.00, 'L', 'A', 599.90, 549.90, 579.90, 649.90, 3.20, 7.50,
     NULL, 9, 8, FALSE, 4, 0.9876, 'catalog', 'https://ml/1'),
    ('run-pub', '2026-08', 'ML', 'PRODUCT', 'MLB2', 'Micro-ondas 30L', 'micro-ondas 30l', 3, 'TOP10', NULL,
     'SEM_HISTORICO', 70.00, 'PRIORIDADE_NORMAL', 'LOW', 'MLB-MICROWAVES', 'MLB1', 'Micro-ondas', 'Electrolux',
     'electrolux', 'ME30', 'electrolux:me30', '127V', 1400, 30.00, 'L', 'A', 799.90, 799.90, 799.90, 799.90, NULL, NULL,
     2, 2, 2, TRUE, 1, 0.5, 'catalog', 'https://ml/2'),
    ('run-pub', '2026-08', 'ML', 'PRODUCT', 'MLB3', 'Air fryer 4L', 'air fryer 4l', 2, 'TOP10', -1,
     'QUEDA', 75.00, 'PRIORIDADE_NORMAL', 'HIGH', 'MLB-AIR_FRYERS', 'MLB9', 'Air fryer', 'Mondial',
     'mondial', 'AF4', 'mondial:af4', '127V', 1500, 4.00, 'L', 'A', 299.90, 250.00, 299.00, 350.00, -3.00, 2.00,
     40, 20, 18, FALSE, 12, 0.7, 'catalog', 'https://ml/3'),
    ('run-old', '2026-07', 'ML', 'PRODUCT', 'MLB1', 'Micro-ondas 20L', 'micro-ondas 20l', 3, 'TOP10', NULL,
     'SEM_HISTORICO', 60.00, 'PRIORIDADE_NORMAL', 'LOW', 'MLB-MICROWAVES', 'MLB1', 'Micro-ondas', 'Electrolux',
     'electrolux', 'ME20', 'electrolux:me20', '220V', 1200, 20.00, 'L', 'A', 559.90, 529.90, 559.90, 599.90, NULL, NULL,
     10, 10, 10, TRUE, 5, 0.9, 'catalog', 'https://ml/1');

INSERT INTO market_price_movement (run_id, domain_id, bucket_key, previous_bucket_key, consecutive, paired_count,
    current_product_count, previous_product_count, current_cohort_median, previous_cohort_median, price_index_delta_pct) VALUES
    ('run-pub', 'MLB-MICROWAVES', '2026-08', '2026-07', TRUE, 4, 6, 5, 599.00, 540.00, 10.90),
    ('run-pub', 'MLB-AIR_FRYERS', '2026-08', '2026-07', TRUE, 3, 5, 5, 299.00, 320.00, -6.50),
    ('run-pub', 'MLB-FANS', '2026-08', '2026-07', TRUE, 2, 4, 4, 150.00, 140.00, 7.10),
    ('run-pub', 'MLB-BLENDERS', '2026-08', '2026-05', FALSE, 5, 6, 6, 200.00, 220.00, -9.00),
    ('run-pub', 'MLB-IRONS', '2026-08', NULL, FALSE, 0, 7, 0, NULL, NULL, NULL),
    ('run-old', 'MLB-MICROWAVES', '2026-07', '2026-06', TRUE, 9, 9, 9, 540.00, 500.00, 8.00);
