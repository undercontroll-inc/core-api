package com.undercontroll.application.mapper;

import com.undercontroll.application.dto.dashboard.CustomerTypeResponse;
import com.undercontroll.application.dto.dashboard.OrdersByStatusResponse;
import com.undercontroll.application.dto.dashboard.RevenueEvolutionResponse;
import com.undercontroll.application.dto.dashboard.TopAppliancesResponse;
import com.undercontroll.application.dto.dashboard.TopComponentsResponse;
import com.undercontroll.domain.model.dashboard.ApplianceCount;
import com.undercontroll.domain.model.dashboard.ComponentUsage;
import com.undercontroll.domain.model.dashboard.CustomerTypePoint;
import com.undercontroll.domain.model.dashboard.RevenuePoint;
import com.undercontroll.domain.model.dashboard.StatusCount;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DashboardDtoMapper {

    List<RevenueEvolutionResponse.DataPoint> toRevenuePoints(List<RevenuePoint> points);

    List<CustomerTypeResponse.DataPoint> toCustomerTypePoints(List<CustomerTypePoint> points);

    List<OrdersByStatusResponse.StatusCount> toStatusCounts(List<StatusCount> counts);

    List<TopAppliancesResponse.ApplianceCount> toApplianceCounts(List<ApplianceCount> counts);

    List<TopComponentsResponse.ComponentUsage> toComponentUsages(List<ComponentUsage> usages);
}
