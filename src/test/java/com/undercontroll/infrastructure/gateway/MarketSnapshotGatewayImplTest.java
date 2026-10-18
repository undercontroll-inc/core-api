package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.exception.MarketDataUnavailableException;
import com.undercontroll.domain.model.market.PublishedMarketSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarketSnapshotGatewayImplTest {

    private EmbeddedDatabase database;
    private JdbcTemplate jdbc;
    private MarketSnapshotGatewayImpl gateway;

    @BeforeEach
    void setUp() {
        database = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .generateUniqueName(true)
                .addScript("schema.sql")
                .build();
        jdbc = new JdbcTemplate(database);
        gateway = new MarketSnapshotGatewayImpl(jdbc);
    }

    @AfterEach
    void tearDown() {
        database.shutdown();
    }

    @Test
    @DisplayName("no published snapshot means the API is still awaiting data")
    void emptyBeforeFirstPublication() {
        assertTrue(gateway.findPublished().isEmpty());
    }

    @Test
    @DisplayName("the latest publication wins, ties broken by snapshot id")
    void latestPublication() {
        publish("run-a", "2026-08", "SUCCESS", "2026-09-01T03:00:00Z");
        publish("run-b", "2026-09", "PARTIAL", "2026-10-01T03:00:00Z");
        publish("run-c", "2026-09", "SUCCESS", "2026-10-01T03:00:00Z");

        PublishedMarketSnapshot snapshot = gateway.findPublished().orElseThrow();

        assertEquals("run-c", snapshot.runId());
        assertEquals("2026-09", snapshot.bucketKey());
        assertEquals(3L, snapshot.snapshotId());
        assertEquals(Instant.parse("2026-10-01T03:00:00Z"), snapshot.publishedAt());
        assertFalse(snapshot.partial());
    }

    @Test
    @DisplayName("a partial run is flagged so it can be disclosed as a limitation")
    void partialRun() {
        publish("run-a", "2026-09", "PARTIAL", "2026-10-01T03:00:00Z");

        assertTrue(gateway.findPublished().orElseThrow().partial());
    }

    @Test
    @DisplayName("a database failure is reported as unavailable, never as missing data")
    @SuppressWarnings("unchecked")
    void failureIsUnavailable() {
        JdbcTemplate broken = mock(JdbcTemplate.class);
        when(broken.query(anyString(), any(RowMapper.class))).thenThrow(new DataAccessResourceFailureException("down"));

        MarketSnapshotGatewayImpl failing = new MarketSnapshotGatewayImpl(broken);

        assertThrows(MarketDataUnavailableException.class, failing::findPublished);
    }

    private void publish(String runId, String bucketKey, String status, String publishedAt) {
        jdbc.update("""
                INSERT INTO market_published_snapshot (run_id, bucket_key, status, quality_report, published_at)
                VALUES (?, ?, ?, '{"verdict":"PASS"}' FORMAT JSON, CAST(? AS TIMESTAMP WITH TIME ZONE))
                """, runId, bucketKey, status, publishedAt);
    }
}
