package com.undercontroll.infrastructure.web.dto;

import com.undercontroll.application.dto.UserDto;

public record AuthUserResponse(
        String token,
        UserDto user
) {
}
