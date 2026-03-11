package com.undercontroll.application.usecase.component;

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
