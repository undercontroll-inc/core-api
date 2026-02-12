package com.undercontroll.domain.port.in;

import com.undercontroll.infrastructure.web.dto.ComponentDto;

public interface RegisterComponentPort {
    record Input(
            String item,
            String description,
            String brand,
            Double price,
            String supplier,
            String category,
            Integer quantity
    ) {}

    record Output(
            String item,
            String description,
            String brand,
            Double price,
            String supplier,
            String category
    ) {}

    Output execute(Input input);
}
