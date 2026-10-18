package com.undercontroll.domain.usecase.order;

import com.undercontroll.domain.enums.OrderStatus;

import java.util.List;

public interface UpdateOrderPort {

    void execute(Integer orderId, Input input);

    record Input(
            OrderStatus status,
            List<Appliance> appliances,
            List<OrderPartInput> parts,
            String customerDescription,
            String technicalDescription
    ) {
    }

    record Appliance(
            Integer id,
            String type,
            String brand,
            String model,
            String volt,
            String series,
            Double laborValue
    ) {
    }
}
