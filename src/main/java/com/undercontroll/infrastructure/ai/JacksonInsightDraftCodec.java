package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.gateway.InsightDraftCodec;
import com.undercontroll.domain.model.insight.InsightDraft;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JacksonInsightDraftCodec implements InsightDraftCodec {

    private final ObjectMapper objectMapper;

    @Override
    public String encode(InsightDraft draft) {
        try {
            return objectMapper.writeValueAsString(InsightDraftJson.from(draft));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize insight draft", ex);
        }
    }

    @Override
    public InsightDraft decode(String json) {
        try {
            return objectMapper.readValue(json, InsightDraftJson.class).toDomain();
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Stored insight draft is unreadable", ex);
        }
    }
}
