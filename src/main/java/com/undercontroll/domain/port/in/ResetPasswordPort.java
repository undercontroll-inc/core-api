package com.undercontroll.domain.port.in;

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
