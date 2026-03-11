package com.undercontroll.application.usecase.component;

import com.undercontroll.application.dto.ComponentDto;

import java.util.List;

public interface GetComponentsByNamePort {
    record Input(
            String name
    ) {}

    record Output(
            List<ComponentDto> components
    ) {}

    Output execute(Input input);
}
