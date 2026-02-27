package com.undercontroll.infrastructure.web.dto;

import com.undercontroll.domain.model.enums.PasswordEventType;

public record CreatePasswordEventRequest(
        PasswordEventType type,
        String agent,
        String userPhone,
        String value
) {
}
