package com.undercontroll.domain.port.in;

import com.undercontroll.infrastructure.web.dto.ComponentDto;

import java.util.List;

public interface GetComponentsPort {
    record Input() {}

    record Output(
            List<ComponentDto> components
    ) {}

    Output execute(Input input);
}
