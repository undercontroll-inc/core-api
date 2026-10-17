package com.undercontroll.infrastructure.auth;

import com.undercontroll.domain.exception.InvalidTokenException;
import com.undercontroll.domain.enums.UserType;
import com.undercontroll.domain.gateway.RefreshTokenGateway;
import com.undercontroll.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import com.undercontroll.infrastructure.persistence.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenAdapter implements RefreshTokenGateway {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiration-days:7}")
    private long refreshTokenExpirationDays;

    @Override
    @Transactional
    public String createRefreshToken(Integer userId, String userEmail, UserType userType) {
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
    @Transactional
    public RefreshTokenData consumeRefreshToken(String token) {
        RefreshTokenJpaEntity entity = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found");
                    return new InvalidTokenException("Refresh token not found");
                });

        if (entity.getExpiresAt().isBefore(Instant.now())) {
            log.warn("Refresh token expired userId={}", entity.getUserId());
            throw new InvalidTokenException("Refresh token has expired");
        }

        if (entity.isRevoked()) {
            log.warn("Refresh token reused userId={}", entity.getUserId());
            refreshTokenRepository.revokeAllByUserId(entity.getUserId());
            throw new InvalidTokenException(
                    "Refresh token has been reused",
                    InvalidTokenException.REFRESH_TOKEN_REUSED
            );
        }

        int updated = refreshTokenRepository.revokeIfActive(token);
        if (updated == 0) {
            log.warn("Refresh token reused userId={}", entity.getUserId());
            refreshTokenRepository.revokeAllByUserId(entity.getUserId());
            throw new InvalidTokenException(
                    "Refresh token has been reused",
                    InvalidTokenException.REFRESH_TOKEN_REUSED
            );
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
