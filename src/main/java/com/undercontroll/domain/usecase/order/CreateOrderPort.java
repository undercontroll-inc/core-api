package com.undercontroll.domain.usecase.order;

import java.util.List;

public interface CreateOrderPort {

    OrderOutput execute(Input input);

    record Input(
            Integer userId,
            List<Appliance> appliances,
            List<OrderPartInput> parts,
            Double discount,
            String receivedAt,
            String deadline,
            String customerDescription,
            String technicalDescription,
            String status,
            boolean returnGuarantee,
            boolean fabricGuarantee,
            String nf
    ) {
    }

    record Appliance(
            String type,
            String brand,
            String model,
            String voltage,
            String serial,
            Double laborValue
    ) {
    }
}
