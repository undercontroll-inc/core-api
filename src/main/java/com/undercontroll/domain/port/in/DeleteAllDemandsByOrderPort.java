package com.undercontroll.domain.port.in;

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
