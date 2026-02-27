package com.undercontroll.domain.port.in;

public interface DeleteUserPort {
    record Input(
            Integer userId
    ) {}

    record Output(
            Boolean success,
            String message
    ) {}

    Output execute(Input input);
}
