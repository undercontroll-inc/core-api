package com.undercontroll.application.usecase.auth;

import com.undercontroll.domain.enums.PasswordEventType;

public interface CreatePasswordEventPort {
    record Input(
            PasswordEventType type,
            String agent,
            String userPhone,
            String value
    ) {}

    record Output(
            String id,
            PasswordEventType type,
            String value,
            String userPhone
    ) {}

    Output execute(Input input);
}
