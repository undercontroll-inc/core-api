package com.undercontroll.application.usecase.component;

import com.undercontroll.application.dto.ComponentDto;

import java.util.List;

public interface GetComponentsByCategoryPort {
    record Input(
            String category
    ) {}

    record Output(
            List<ComponentDto> components
    ) {}

    Output execute(Input input);
}
