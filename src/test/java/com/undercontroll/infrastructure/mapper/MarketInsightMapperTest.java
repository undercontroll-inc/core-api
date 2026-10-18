package com.undercontroll.infrastructure.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.enums.InsightGenerationStatus;
import com.undercontroll.domain.model.MarketMonthlyInsight;
import com.undercontroll.domain.model.insight.InsightsPayload;
import com.undercontroll.infrastructure.persistence.entity.MarketMonthlyInsightJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(MarketInsightMapperTest.Config.class)
class MarketInsightMapperTest {

    @Configuration
    @Import({MarketInsightMapperImpl.class, InsightsPayloadCodec.class, InsightsPayloadJsonMapperImpl.class})
    static class Config {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Autowired
    private MarketInsightMapper mapper;

    @Test
    @DisplayName("stores the payload as snake_case JSON and reads it back into the domain record")
    void payloadRoundTrip() {
        InsightsPayload payload = new InsightsPayload(
                new InsightsPayload.Periodo("2026-08", null),
                new InsightsPayload.CoberturaMatch(1, 0, 0, 2),
                List.of(),
                List.of(),
                List.of("amostra")
        );
        MarketMonthlyInsight insight = MarketMonthlyInsight.builder()
                .id(4)
                .bucketKey("2026-08")
                .status(InsightGenerationStatus.SUCCESS)
                .payload(payload)
                .promptVersion("6")
                .build();

        MarketMonthlyInsightJpaEntity entity = mapper.toEntityWithId(insight);

        assertTrue(entity.getPayload().contains("\"bucket_atual\":\"2026-08\""));
        assertEquals(payload, mapper.toDomain(entity).getPayload());
        assertEquals("2026-08", mapper.toDomain(entity).getBucketKey());
    }

    @Test
    @DisplayName("failed rows without payload map to a null payload")
    void failedRow() {
        MarketMonthlyInsightJpaEntity entity = MarketMonthlyInsightJpaEntity.builder()
                .bucketKey("2026-08")
                .status(InsightGenerationStatus.FAILED)
                .errorMessage("timeout")
                .build();

        MarketMonthlyInsight insight = mapper.toDomain(entity);

        assertNull(insight.getPayload());
        assertEquals("timeout", insight.getErrorMessage());
        assertNull(mapper.toEntityWithId(insight).getPayload());
    }
}
