package com.undercontroll.application.usecase.component;

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
