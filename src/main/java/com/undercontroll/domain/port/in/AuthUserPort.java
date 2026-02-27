package com.undercontroll.domain.port.in;

import com.undercontroll.application.dto.UserDto;

public interface AuthUserPort {
    record Input(
            String email,
            String password
    ) {}

    record Output(
            String token,
            UserDto user
    ) {}

    Output execute(Input input);
}
