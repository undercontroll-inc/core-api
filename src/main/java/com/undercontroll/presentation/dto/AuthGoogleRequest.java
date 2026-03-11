package com.undercontroll.presentation.dto;

public record AuthGoogleRequest(
        String email,
        String token
) {
}
