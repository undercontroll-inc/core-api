package com.undercontroll.application.mapper;

import com.undercontroll.application.dto.component.ComponentDto;
import com.undercontroll.application.dto.component.RegisterComponentRequest;
import com.undercontroll.application.dto.component.UpdateComponentRequest;
import com.undercontroll.domain.usecase.component.ComponentOutput;
import com.undercontroll.domain.usecase.component.RegisterComponentPort;
import com.undercontroll.domain.usecase.component.UpdateComponentPort;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ComponentPartDtoMapper {

    ComponentDto toDto(ComponentOutput output);

    RegisterComponentPort.Input toInput(RegisterComponentRequest request);

    UpdateComponentPort.Input toInput(UpdateComponentRequest request);
}
