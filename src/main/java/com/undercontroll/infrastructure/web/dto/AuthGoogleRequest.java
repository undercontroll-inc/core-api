package com.undercontroll.infrastructure.web.dto;

public record AuthGoogleRequest(
        String email,
        String token
) {
}
