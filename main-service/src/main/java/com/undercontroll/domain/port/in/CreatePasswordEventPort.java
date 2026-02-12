package com.undercontroll.domain.port.in;

import com.undercontroll.domain.entity.enums.PasswordEventType;
import com.undercontroll.infrastructure.web.dto.CreatePasswordEventRequest;

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
