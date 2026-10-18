package com.undercontroll.application.mapper;

import com.undercontroll.application.dto.analytics.MarketAnalyticsResponse;
import com.undercontroll.domain.usecase.analytics.GetMarketAnalyticsPort;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MarketAnalyticsDtoMapper {

    MarketAnalyticsResponse toResponse(GetMarketAnalyticsPort.Output output);
}
