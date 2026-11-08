package com.undercontroll.domain.usecase.order;


import java.util.List;

public interface UpdateOrderPort {

    OrderOutput execute(Integer orderId, Input input);

    record Input(
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
