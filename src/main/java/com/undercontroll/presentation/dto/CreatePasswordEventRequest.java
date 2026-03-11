package com.undercontroll.presentation.dto;

import com.undercontroll.domain.enums.PasswordEventType;

public record CreatePasswordEventRequest(
        PasswordEventType type,
        String agent,
        String userPhone,
        String value
) {
}
