package com.undercontroll.domain.usecase.component;

import com.undercontroll.domain.model.ComponentPart;

public record ComponentOutput(
        Integer id,
        String item,
        String description,
        String brand,
        Double price,
        Long quantity,
        String supplier,
        String category
) {

    public static ComponentOutput from(ComponentPart component) {
        if (component == null) {
            return null;
        }
        return new ComponentOutput(
                component.getId(),
                component.getName(),
                component.getDescription(),
                component.getBrand(),
                component.getPrice(),
                component.getQuantity(),
                component.getSupplier(),
                component.getCategory()
        );
    }
}
