package com.undercontroll.infrastructure.gateway;

import com.undercontroll.domain.exception.MarketDataUnavailableException;
import com.undercontroll.domain.gateway.MarketSnapshotGateway;
import com.undercontroll.domain.model.market.PublishedMarketSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class MarketSnapshotGatewayImpl implements MarketSnapshotGateway {

    static final String PUBLISHED_SNAPSHOT = """
            SELECT snapshot_id, run_id, bucket_key, status, published_at
            FROM vw_market_published_snapshot
            """;

    private static final RowMapper<PublishedMarketSnapshot> SNAPSHOT = (rs, rowNum) -> {
        OffsetDateTime publishedAt = rs.getObject("published_at", OffsetDateTime.class);
        return new PublishedMarketSnapshot(
                rs.getLong("snapshot_id"),
                rs.getString("run_id"),
                rs.getString("bucket_key"),
                rs.getString("status"),
                publishedAt == null ? null : publishedAt.toInstant()
        );
    };

    private final JdbcTemplate jdbc;

    @Override
    public Optional<PublishedMarketSnapshot> findPublished() {
        try {
            List<PublishedMarketSnapshot> rows = jdbc.query(PUBLISHED_SNAPSHOT, SNAPSHOT);
            return rows.stream().findFirst();
        } catch (DataAccessException ex) {
            log.warn("Published market snapshot query failed: {}", ex.getClass().getSimpleName());
            throw new MarketDataUnavailableException(ex);
        }
    }
}
