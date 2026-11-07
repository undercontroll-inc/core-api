package com.undercontroll.domain.usecase.component;

public interface UpdateComponentPort {

    ComponentOutput execute(Integer componentId, Input input);

    record Input(
            String item,
            String description,
            String brand,
            Double price,
            String supplier,
            String category
    ) {
    }
}
