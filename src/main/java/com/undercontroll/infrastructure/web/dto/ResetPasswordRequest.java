package com.undercontroll.infrastructure.web.dto;

public record ResetPasswordRequest(
        String newPassword,
        boolean inFirstLogin
) {
}
