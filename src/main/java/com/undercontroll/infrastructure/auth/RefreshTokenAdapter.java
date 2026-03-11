package com.undercontroll.infrastructure.auth;

import com.undercontroll.domain.exception.InvalidTokenException;
import com.undercontroll.domain.enums.UserType;
import com.undercontroll.application.port.RefreshTokenPort;
import com.undercontroll.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import com.undercontroll.infrastructure.persistence.repository.jpa.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class RefreshTokenAdapter implements RefreshTokenPort {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiration-days:7}")
    private long refreshTokenExpirationDays;

    @Override
    @Transactional
    public String createRefreshToken(Integer userId, String userEmail, UserType userType) {
        // Revoke all existing tokens for this user before issuing a new one
        refreshTokenRepository.revokeAllByUserId(userId);

        byte[] randomBytes = new byte[64];
        SECURE_RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        RefreshTokenJpaEntity entity = RefreshTokenJpaEntity.builder()
                .token(token)
                .userId(userId)
                .userEmail(userEmail)
                .userRole(userType.name())
                .expiresAt(Instant.now().plusSeconds(refreshTokenExpirationDays * 24 * 60 * 60))
                .revoked(false)
                .build();

        refreshTokenRepository.save(entity);
        return token;
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshTokenData validateRefreshToken(String token) {
        RefreshTokenJpaEntity entity = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (entity.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        if (entity.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        return new RefreshTokenData(
                entity.getToken(),
                entity.getUserId(),
                entity.getUserEmail(),
                entity.getUserRole(),
                entity.getExpiresAt()
        );
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(Integer userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}

