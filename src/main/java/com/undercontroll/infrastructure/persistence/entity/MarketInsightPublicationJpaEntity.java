package com.undercontroll.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "market_insight_publication")
public class MarketInsightPublicationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, unique = true)
    private Long jobId;

    @Column(name = "attempt_id", nullable = false)
    private Long attemptId;

    @Column(name = "snapshot_id", nullable = false)
    private Long snapshotId;

    @Column(name = "bucket_key", nullable = false, length = 10)
    private String bucketKey;

    @Column(name = "comparison_bucket_key", length = 10)
    private String comparisonBucketKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(length = 20)
    private String provider;

    @Column(length = 100)
    private String model;

    @Column(name = "prompt_version", nullable = false, length = 32)
    private String promptVersion;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;
}
