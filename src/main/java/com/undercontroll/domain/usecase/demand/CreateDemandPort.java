package com.undercontroll.domain.usecase.demand;

public interface CreateDemandPort {

    DemandOutput execute(Integer orderId, Input input);

    record Input(Integer componentPartId, Long quantity) {
    }
}
