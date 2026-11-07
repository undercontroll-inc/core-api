package com.undercontroll.domain.usecase.component;

public interface RegisterComponentPort {

    ComponentOutput execute(Input input);

    record Input(
            String item,
            String description,
            String brand,
            String category,
            Integer quantity,
            Double price,
            String supplier
    ) {
    }
}
