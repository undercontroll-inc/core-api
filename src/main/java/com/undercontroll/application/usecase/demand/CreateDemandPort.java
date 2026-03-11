package com.undercontroll.application.usecase.demand;

public interface CreateDemandPort {
    record Input(
            Integer componentPartId,
            Long quantity,
            Integer orderId
    ) {}

    record Output(
            Integer id,
            Integer componentId,
            Integer orderId,
            Long quantity
    ) {}

    Output execute(Input input);
}
