package com.undercontroll.application.usecase.demand;

public interface DeleteAllDemandsByOrderPort {
    record Input(
            Integer orderId
    ) {}

    record Output(
            Boolean success,
            String message
    ) {}

    Output execute(Input input);
}
