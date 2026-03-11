package com.undercontroll.presentation.dto;

public record ResetPasswordRequest(
        String newPassword,
        boolean inFirstLogin
) {
}
