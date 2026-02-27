package com.undercontroll.domain.port.in;

public interface DeleteDemandPort {
    record Input(
            Integer demandId
    ) {}

    record Output(
            Boolean success,
            String message
    ) {}

    Output execute(Input input);
}
