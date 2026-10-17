package com.undercontroll.infrastructure.auth;

import com.undercontroll.domain.enums.UserType;
import com.undercontroll.domain.exception.InvalidTokenException;
import com.undercontroll.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import com.undercontroll.infrastructure.persistence.repository.RefreshTokenRepository;
import com.undercontroll.domain.gateway.RefreshTokenGateway.RefreshTokenData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenAdapterTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RefreshTokenAdapter(refreshTokenRepository);
        ReflectionTestUtils.setField(adapter, "refreshTokenExpirationDays", 7L);
    }

    @Test
    @DisplayName("createRefreshToken persists a new token without revoking other sessions")
    void createDoesNotRevokeOtherSessions() {
        when(refreshTokenRepository.save(any(RefreshTokenJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String token = adapter.createRefreshToken(7, "user@example.com", UserType.CUSTOMER);

        assertThat(token).isNotBlank();
        verify(refreshTokenRepository, never()).revokeAllByUserId(7);
        ArgumentCaptor<RefreshTokenJpaEntity> captor = ArgumentCaptor.forClass(RefreshTokenJpaEntity.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7);
        assertThat(captor.getValue().isRevoked()).isFalse();
        assertThat(captor.getValue().getToken()).isEqualTo(token);
    }

    @Test
    @DisplayName("consumeRefreshToken revokes only the presented active token")
    void consumeRevokesOnlyThePresentedToken() {
        RefreshTokenJpaEntity entity = activeToken("current-refresh", 7);

        when(refreshTokenRepository.findByToken("current-refresh")).thenReturn(Optional.of(entity));
        when(refreshTokenRepository.revokeIfActive("current-refresh")).thenReturn(1);

        RefreshTokenData data = adapter.consumeRefreshToken("current-refresh");

        assertThat(data.userId()).isEqualTo(7);
        assertThat(data.userEmail()).isEqualTo("user@example.com");
        verify(refreshTokenRepository).revokeIfActive("current-refresh");
        verify(refreshTokenRepository, never()).revokeAllByUserId(7);
    }

    @Test
    @DisplayName("expired refresh token is rejected without reuse detection")
    void expiredRefreshTokenIsRejected() {
        RefreshTokenJpaEntity entity = RefreshTokenJpaEntity.builder()
                .token("expired-refresh")
                .userId(7)
                .userEmail("user@example.com")
                .userRole(UserType.CUSTOMER.name())
                .expiresAt(Instant.now().minusSeconds(60))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByToken("expired-refresh")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> adapter.consumeRefreshToken("expired-refresh"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Refresh token has expired")
                .extracting(ex -> ((InvalidTokenException) ex).getErrorCode())
                .isEqualTo(InvalidTokenException.INVALID_TOKEN);
        verify(refreshTokenRepository, never()).revokeIfActive(any());
        verify(refreshTokenRepository, never()).revokeAllByUserId(any());
    }

    @Test
    @DisplayName("reusing a revoked refresh token revokes every session for that user")
    void reusedRefreshTokenRevokesAllSessions() {
        RefreshTokenJpaEntity entity = RefreshTokenJpaEntity.builder()
                .token("stolen-refresh")
                .userId(7)
                .userEmail("user@example.com")
                .userRole(UserType.CUSTOMER.name())
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByToken("stolen-refresh")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> adapter.consumeRefreshToken("stolen-refresh"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Refresh token has been reused")
                .extracting(ex -> ((InvalidTokenException) ex).getErrorCode())
                .isEqualTo(InvalidTokenException.REFRESH_TOKEN_REUSED);
        verify(refreshTokenRepository).revokeAllByUserId(7);
    }

    @Test
    @DisplayName("losing the revoke race is treated as reuse")
    void revokeRaceIsTreatedAsReuse() {
        RefreshTokenJpaEntity entity = activeToken("racing-refresh", 7);

        when(refreshTokenRepository.findByToken("racing-refresh")).thenReturn(Optional.of(entity));
        when(refreshTokenRepository.revokeIfActive("racing-refresh")).thenReturn(0);

        assertThatThrownBy(() -> adapter.consumeRefreshToken("racing-refresh"))
                .isInstanceOf(InvalidTokenException.class)
                .extracting(ex -> ((InvalidTokenException) ex).getErrorCode())
                .isEqualTo(InvalidTokenException.REFRESH_TOKEN_REUSED);
        verify(refreshTokenRepository).revokeAllByUserId(7);
    }

    private static RefreshTokenJpaEntity activeToken(String token, Integer userId) {
        return RefreshTokenJpaEntity.builder()
                .token(token)
                .userId(userId)
                .userEmail("user@example.com")
                .userRole(UserType.CUSTOMER.name())
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();
    }
}
