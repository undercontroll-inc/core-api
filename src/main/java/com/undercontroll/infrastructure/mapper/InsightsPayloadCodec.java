package com.undercontroll.infrastructure.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.domain.exception.InsightPayloadUnreadableException;
import com.undercontroll.domain.model.insight.InsightsPayload;
import com.undercontroll.infrastructure.ai.InsightsPayloadJson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InsightsPayloadCodec {

    private final ObjectMapper objectMapper;
    private final InsightsPayloadJsonMapper jsonMapper;

    public String encode(InsightsPayload payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(jsonMapper.toJson(payload));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize insights payload", ex);
        }
    }

    public InsightsPayload decode(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return jsonMapper.toDomain(objectMapper.readValue(json, InsightsPayloadJson.class));
        } catch (JsonProcessingException ex) {
            throw new InsightPayloadUnreadableException(ex);
        }
    }
}
