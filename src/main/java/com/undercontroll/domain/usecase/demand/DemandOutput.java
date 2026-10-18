package com.undercontroll.domain.usecase.demand;

import com.undercontroll.domain.model.Demand;

public record DemandOutput(Integer id, Integer componentId, Integer orderId, Long quantity) {

    public static DemandOutput from(Demand demand) {
        return new DemandOutput(
                demand.getId(),
                demand.getComponent().getId(),
                demand.getOrder().getId(),
                demand.getQuantity()
        );
    }
}
