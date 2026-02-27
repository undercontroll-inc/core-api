package com.undercontroll.domain.port.in;

import com.undercontroll.application.dto.UserDto;

import java.util.List;

public interface GetUsersPort {
    record Input() {}

    record Output(
            List<UserDto> users
    ) {}

    Output execute(Input input);
}
