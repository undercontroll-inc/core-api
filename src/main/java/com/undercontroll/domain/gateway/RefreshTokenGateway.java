package com.undercontroll.domain.gateway;

import com.undercontroll.domain.enums.UserType;

import java.time.Instant;

public interface RefreshTokenGateway {

    record RefreshTokenData(
            String token,
            Integer userId,
            String userEmail,
            String userRole,
            Instant expiresAt
    ) {}

    String createRefreshToken(Integer userId, String userEmail, UserType userType);

    RefreshTokenData consumeRefreshToken(String token);

    void revokeAllUserTokens(Integer userId);
}
