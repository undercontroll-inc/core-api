package com.undercontroll.infrastructure.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.undercontroll.domain.exception.MarketDataUnavailableException;
import org.junit.jupiter.api.function.Executable;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MarketViewGatewayImplTest {

    @Mock
    private NamedParameterJdbcTemplate jdbc;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private MarketViewGatewayImpl gateway;

    @Test
    @DisplayName("propagates view failures instead of returning empty market data")
    void failuresPropagate() {
        when(jdbc.getJdbcTemplate()).thenReturn(jdbcTemplate);
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class)))
                .thenThrow(new InvalidDataAccessResourceUsageException("missing view"));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenThrow(new InvalidDataAccessResourceUsageException("missing view"));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenThrow(new InvalidDataAccessResourceUsageException("missing view"));
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenThrow(new InvalidDataAccessResourceUsageException("missing view"));
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(String.class)))
                .thenThrow(new InvalidDataAccessResourceUsageException("missing view"));

        List<Executable> calls = List.of(
                gateway::findCurrentBucketKey,
                gateway::countCurrentProducts,
                gateway::countDistinctBrands,
                gateway::findAllCurrentProducts,
                () -> gateway.findTopBrands("2026-08", 5),
                () -> gateway.findTopCategories("2026-08", 5),
                () -> gateway.findPriceMovements("2026-08"),
                () -> gateway.findStockOpportunities("2026-08"),
                gateway::findRisingProductsHighConfidence,
                () -> gateway.findBrandMomentum("2026-08"),
                () -> gateway.findCategorySummary("2026-08"),
                gateway::findPriceDispersion,
                () -> gateway.findUncoveredCategories("2026-08", List.of("MLB-MICROWAVES")),
                () -> gateway.findPreviousBucketKey("2026-08")
        );

        calls.forEach(call -> assertThrows(MarketDataUnavailableException.class, call));
    }

    @Test
    @DisplayName("no row is an absent bucket or a zero count, not a failure")
    void emptyResultIsAbsence() {
        when(jdbc.getJdbcTemplate()).thenReturn(jdbcTemplate);
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class))).thenThrow(new EmptyResultDataAccessException(1));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(null);
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(null);

        assertTrue(gateway.findCurrentBucketKey().isEmpty());
        assertTrue(gateway.findPreviousBucketKey(null).isEmpty());
        assertEquals(0L, gateway.countCurrentProducts());
        assertTrue(gateway.findTopBrands("2026-08", 5).isEmpty());
    }
}
