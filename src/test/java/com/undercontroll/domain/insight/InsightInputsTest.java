package com.undercontroll.domain.insight;

import com.undercontroll.domain.model.insight.InsightGenerationInput;
import com.undercontroll.domain.model.insight.InsightMarketData;
import com.undercontroll.domain.model.insight.InsightsPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.undercontroll.domain.insight.InsightMarketFixtures.BUCKET;
import static com.undercontroll.domain.insight.InsightMarketFixtures.data;
import static com.undercontroll.domain.insight.InsightMarketFixtures.product;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsightInputsTest {

    @Test
    @DisplayName("input carries candidates, comparison, coverage and server-side rising products")
    void build() {
        InsightGenerationInput input = InsightInputs.build(data(),
                List.of(product("MLB9", "X", "electrolux", "MLB-MICROWAVES", 10, 20, 5, false)), false);

        assertEquals(BUCKET, input.bucketKey());
        assertEquals("2026-07", input.comparisonBucketKey());
        assertEquals(1, input.coverage().category() + input.coverage().brandCategory());
        assertEquals(12, input.candidates().size());
        assertEquals(List.of(InsightInputs.POPULARITY), input.limitations());
        InsightsPayload.ProdutoEmAlta first = input.risingProducts().getFirst();
        assertEquals("Micro-ondas Electrolux 20L", first.nome());
        assertEquals(579.9, first.precoMediano());
        assertEquals(8, first.precosValidos());
        assertTrue(first.atendidoPeloCliente());
        InsightsPayload.ProdutoEmAlta second = input.risingProducts().get(1);
        assertNull(second.precoMediano());
        assertTrue(second.atendidoPeloCliente());
    }

    @Test
    @DisplayName("limitations disclose missing comparison, partial snapshot, empty catalog and no candidates")
    void limitations() {
        InsightMarketData empty = new InsightMarketData(BUCKET, List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of());

        InsightGenerationInput input = InsightInputs.build(empty, List.of(), true);

        assertNull(input.comparisonBucketKey());
        assertEquals(List.of(InsightInputs.POPULARITY, InsightInputs.NO_COMPARISON, InsightInputs.PARTIAL,
                InsightInputs.NO_CATALOG, InsightInputs.NO_CANDIDATES), input.limitations());
        assertFalse(input.candidate("x").isPresent());
    }
}
