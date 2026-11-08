package com.undercontroll.domain.usecase.order;

import com.undercontroll.domain.model.Demand;

public record OrderPartOutput(
        Integer id,
        String item,
        String description,
        String brand,
        Double price,
        Long quantity,
        String supplier,
        String category,
        Long demandQuantity
) {

    public static OrderPartOutput from(Demand demand) {
        var component = demand.getComponent();
        if (component == null) {
            return new OrderPartOutput(null, null, null, null, null, null, null, null, demand.getQuantity());
        }
        return new OrderPartOutput(
                component.getId(),
                component.getName(),
                component.getDescription(),
                component.getBrand(),
                component.getPrice(),
                component.getQuantity(),
                component.getSupplier(),
                component.getCategory(),
                demand.getQuantity()
        );
    }
}
