package com.undercontroll.domain.port.in;

import com.undercontroll.application.dto.UserDto;

import java.util.List;

public interface GetCustomersPort {
    record Input() {}

    record Output(
            List<UserDto> customers
    ) {}

    Output execute(Input input);
}
