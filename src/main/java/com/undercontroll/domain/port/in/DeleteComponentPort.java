package com.undercontroll.domain.port.in;

public interface DeleteComponentPort {
    record Input(
            Integer componentId
    ) {}

    record Output(
            Boolean success,
            String message
    ) {}

    Output execute(Input input);
}
