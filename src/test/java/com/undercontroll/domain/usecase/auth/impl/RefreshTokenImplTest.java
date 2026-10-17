package com.undercontroll.domain.usecase.auth.impl;

import com.undercontroll.application.dto.auth.RefreshTokenRequest;
import com.undercontroll.application.dto.auth.RefreshTokenResponse;
import com.undercontroll.domain.enums.UserType;
import com.undercontroll.domain.gateway.RefreshTokenGateway;
import com.undercontroll.domain.gateway.RefreshTokenGateway.RefreshTokenData;
import com.undercontroll.domain.gateway.TokenGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenImplTest {

    @Mock
    private RefreshTokenGateway refreshTokenGateway;

    @Mock
    private TokenGateway tokenGateway;

    @InjectMocks
    private RefreshTokenImpl useCase;

    @Test
    @DisplayName("consumes the presented refresh token, then issues a new pair")
    void rotatesRefreshTokenWithoutRevokingOtherSessions() {
        RefreshTokenData current = new RefreshTokenData(
                "old-refresh",
                42,
                "user@example.com",
                UserType.CUSTOMER.name(),
                Instant.now().plusSeconds(3600)
        );

        when(refreshTokenGateway.consumeRefreshToken("old-refresh")).thenReturn(current);
        when(refreshTokenGateway.createRefreshToken(42, "user@example.com", UserType.CUSTOMER))
                .thenReturn("new-refresh");
        when(tokenGateway.generateToken("42", UserType.CUSTOMER)).thenReturn("new-access");

        RefreshTokenResponse response = useCase.execute(new RefreshTokenRequest("old-refresh"));

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        verify(refreshTokenGateway).consumeRefreshToken("old-refresh");
        verify(refreshTokenGateway).createRefreshToken(42, "user@example.com", UserType.CUSTOMER);
        verify(tokenGateway).generateToken("42", UserType.CUSTOMER);
        verifyNoMoreInteractions(refreshTokenGateway);
    }
}
