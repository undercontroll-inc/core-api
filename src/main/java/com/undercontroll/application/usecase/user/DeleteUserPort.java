package com.undercontroll.application.usecase.user;

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
