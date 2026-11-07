package com.undercontroll.infrastructure.mapper;

import com.undercontroll.domain.model.insight.InsightAttempt;
import com.undercontroll.domain.model.insight.InsightJob;
import com.undercontroll.domain.model.insight.MarketInsightPublication;
import com.undercontroll.infrastructure.persistence.entity.InsightAttemptJpaEntity;
import com.undercontroll.infrastructure.persistence.entity.InsightJobJpaEntity;
import com.undercontroll.infrastructure.persistence.entity.MarketInsightPublicationJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = InsightsPayloadCodec.class)
public interface InsightJobPersistenceMapper {

    InsightJob toDomain(InsightJobJpaEntity entity);

    InsightAttempt toDomain(InsightAttemptJpaEntity entity);

    InsightAttemptJpaEntity toEntity(InsightAttempt attempt);

    MarketInsightPublication toDomain(MarketInsightPublicationJpaEntity entity);

    MarketInsightPublicationJpaEntity toEntity(MarketInsightPublication publication);
}
