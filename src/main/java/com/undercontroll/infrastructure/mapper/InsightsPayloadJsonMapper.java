package com.undercontroll.infrastructure.mapper;

import com.undercontroll.domain.model.insight.InsightsPayload;
import com.undercontroll.infrastructure.ai.InsightsPayloadJson;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InsightsPayloadJsonMapper {

    InsightsPayload toDomain(InsightsPayloadJson json);

    InsightsPayloadJson toJson(InsightsPayload payload);
}
