package com.undercontroll.infrastructure.mapper;

import com.undercontroll.domain.model.MarketMonthlyInsight;
import com.undercontroll.infrastructure.persistence.entity.MarketMonthlyInsightJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = InsightsPayloadCodec.class)
public interface MarketInsightMapper {

    MarketMonthlyInsight toDomain(MarketMonthlyInsightJpaEntity entity);

    MarketMonthlyInsightJpaEntity toEntityWithId(MarketMonthlyInsight domain);
}
