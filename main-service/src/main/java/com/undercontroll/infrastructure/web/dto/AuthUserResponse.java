package com.undercontroll.infrastructure.web.dto;

public record AuthUserResponse(
        String token,
        UserDto user
) {
}
