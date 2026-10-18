package com.undercontroll.infrastructure.ai;

import com.undercontroll.domain.model.insight.InsightPromptContext;
import com.undercontroll.domain.model.market.MatchCoverage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class InsightGenerationContextTest {

    @Test
    @DisplayName("builds generation state from the prompt context")
    void buildsStateFromPrompt() {
        InsightPromptContext prompt = new InsightPromptContext(
                "2026-08", "2026-07", MatchCoverage.empty(), Set.of(), List.of(), List.of());
        EvidenceIndex evidence = new EvidenceIndex(new com.fasterxml.jackson.databind.ObjectMapper());

        InsightGenerationContext.State state = InsightGenerationContext.State.from(prompt, evidence);

        assertEquals("2026-08", state.bucketKey());
        assertEquals("2026-07", state.comparisonBucketKey());
        assertSame(evidence, state.evidence());
    }
}
