package com.undercontroll.application.usecase.auth;

public interface ResetPasswordPort {
    record Input(
            Integer userId,
            String newPassword,
            String token
    ) {}

    record Output(
            Boolean success,
            String message
    ) {}

    Output execute(Input input);
}
