package com.undercontroll.application.port;

import com.undercontroll.domain.enums.UserType;

import java.time.Instant;

public interface RefreshTokenPort {

    record RefreshTokenData(
            String token,
            Integer userId,
            String userEmail,
            String userRole,
            Instant expiresAt
    ) {}

    String createRefreshToken(Integer userId, String userEmail, UserType userType);

    RefreshTokenData validateRefreshToken(String token);

    void revokeAllUserTokens(Integer userId);
}

