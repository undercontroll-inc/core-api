package com.undercontroll.domain.port.in;

import com.undercontroll.application.dto.ComponentDto;

public interface UpdateComponentPort {
    record Input(
            Integer componentId,
            String item,
            String description,
            String brand,
            Double price,
            String supplier,
            String category
    ) {}

    record Output(
            Integer id,
            String name,
            String description,
            String brand,
            Double price,
            Long quantity,
            String supplier,
            String category
    ) {}

    Output execute(Input input);
}
