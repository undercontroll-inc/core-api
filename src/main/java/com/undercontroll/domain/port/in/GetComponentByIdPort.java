package com.undercontroll.domain.port.in;

import com.undercontroll.application.dto.ComponentDto;

public interface GetComponentByIdPort {
    record Input(
            Integer componentId
    ) {}

    record Output(
            ComponentDto component
    ) {}

    Output execute(Input input);
}
