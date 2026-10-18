package com.undercontroll.domain.usecase.auth.impl;

import com.undercontroll.domain.enums.AuthProvider;
import com.undercontroll.domain.enums.UserType;
import com.undercontroll.domain.exception.InvalidAuthException;
import com.undercontroll.domain.gateway.GoogleAuthGateway;
import com.undercontroll.domain.gateway.MetricsGateway;
import com.undercontroll.domain.gateway.RefreshTokenGateway;
import com.undercontroll.domain.gateway.TokenGateway;
import com.undercontroll.domain.gateway.UserGateway;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.usecase.auth.AuthUserPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUserImplTest {

    @Mock
    private UserGateway userGateway;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenGateway tokenGateway;
    @Mock
    private RefreshTokenGateway refreshTokenGateway;
    @Mock
    private MetricsGateway metricsGateway;
    @Mock
    private GoogleAuthGateway googleAuthGateway;

    @InjectMocks
    private AuthUserImpl useCase;

    private final User user = User.builder()
            .id(42)
            .email("ana@x.com")
            .password("hash")
            .name("Ana")
            .userType(UserType.ADMINISTRATOR)
            .build();

    @Test
    @DisplayName("password login issues tokens for the user id and returns the user")
    void passwordLogin() {
        when(userGateway.findByEmail("ana@x.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(tokenGateway.generateToken("42", UserType.ADMINISTRATOR)).thenReturn("access");
        when(refreshTokenGateway.createRefreshToken(42, "ana@x.com", UserType.ADMINISTRATOR)).thenReturn("refresh");

        AuthUserPort.Output output = useCase.execute(new AuthUserPort.Input(AuthProvider.PASSWORD, "ana@x.com", "secret", null));

        assertEquals("access", output.accessToken());
        assertEquals("refresh", output.refreshToken());
        assertEquals(42, output.user().id());
        verify(metricsGateway).incrementLoginSuccess();
    }

    @Test
    @DisplayName("google login verifies the id token against the email")
    void googleLogin() {
        when(userGateway.findByEmail("ana@x.com")).thenReturn(Optional.of(user));
        when(googleAuthGateway.verify("id-token", "ana@x.com")).thenReturn(true);
        when(tokenGateway.generateToken("42", UserType.ADMINISTRATOR)).thenReturn("access");

        AuthUserPort.Output output = useCase.execute(new AuthUserPort.Input(AuthProvider.GOOGLE, "ana@x.com", null, "id-token"));

        assertEquals("access", output.accessToken());
    }

    @Test
    @DisplayName("unknown email, missing provider, wrong password and blank token fail the same way")
    void failures() {
        when(userGateway.findByEmail("nobody@x.com")).thenReturn(Optional.empty());
        when(userGateway.findByEmail("ana@x.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThrows(InvalidAuthException.class, () -> useCase.execute(new AuthUserPort.Input(AuthProvider.PASSWORD, "nobody@x.com", "x", null)));
        assertThrows(InvalidAuthException.class, () -> useCase.execute(new AuthUserPort.Input(null, "ana@x.com", "x", null)));
        assertThrows(InvalidAuthException.class, () -> useCase.execute(new AuthUserPort.Input(AuthProvider.PASSWORD, "ana@x.com", "wrong", null)));
        assertThrows(InvalidAuthException.class, () -> useCase.execute(new AuthUserPort.Input(AuthProvider.PASSWORD, "ana@x.com", " ", null)));
        assertThrows(InvalidAuthException.class, () -> useCase.execute(new AuthUserPort.Input(AuthProvider.GOOGLE, "ana@x.com", null, " ")));

        verify(metricsGateway, org.mockito.Mockito.times(5)).incrementLoginFailed();
        verify(tokenGateway, never()).generateToken(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
