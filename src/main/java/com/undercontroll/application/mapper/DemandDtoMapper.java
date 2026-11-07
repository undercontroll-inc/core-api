package com.undercontroll.application.mapper;

import com.undercontroll.application.dto.demand.CreateDemandRequest;
import com.undercontroll.application.dto.demand.DemandDto;
import com.undercontroll.domain.usecase.demand.CreateDemandPort;
import com.undercontroll.domain.usecase.demand.DemandOutput;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DemandDtoMapper {

    DemandDto toDto(DemandOutput output);

    CreateDemandPort.Input toInput(CreateDemandRequest request);
}
