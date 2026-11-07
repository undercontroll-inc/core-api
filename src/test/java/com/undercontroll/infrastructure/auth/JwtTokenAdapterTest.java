package com.undercontroll.infrastructure.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.undercontroll.domain.enums.UserType;
import com.undercontroll.domain.exception.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenAdapterTest {

    private static final String SECRET = "test-secret-key-for-unit-tests";

    private JwtTokenAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JwtTokenAdapter();
        ReflectionTestUtils.setField(adapter, "secret", SECRET);
        ReflectionTestUtils.setField(adapter, "accessTokenExpirationMinutes", 15L);
    }

    @Test
    @DisplayName("validateToken accepts a token issued by generateToken")
    void roundTrip() {
        String token = adapter.generateToken("42", UserType.CUSTOMER);

        assertThat(adapter.validateToken(token).subject()).isEqualTo("42");
        assertThat(adapter.validateToken(token).role()).isEqualTo("CUSTOMER");
    }

    @Test
    @DisplayName("expired access token is reported as TOKEN_EXPIRED")
    void expiredToken() {
        String token = JWT.create()
                .withIssuer("undercontroll")
                .withClaim("roles", UserType.CUSTOMER.name())
                .withSubject("42")
                .withExpiresAt(Instant.now().minusSeconds(60))
                .sign(Algorithm.HMAC256(SECRET));

        assertThatThrownBy(() -> adapter.validateToken(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Access token has expired")
                .extracting(ex -> ((InvalidTokenException) ex).getErrorCode())
                .isEqualTo(InvalidTokenException.TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("invalid tokens use a generic message without verifier details")
    void invalidTokenDoesNotLeakVerifierMessage() {
        assertThatThrownBy(() -> adapter.validateToken("not-a-jwt"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid token")
                .extracting(ex -> ((InvalidTokenException) ex).getErrorCode())
                .isEqualTo(InvalidTokenException.INVALID_TOKEN);
    }

    @Test
    @DisplayName("blank tokens are rejected")
    void blankToken() {
        assertThatThrownBy(() -> adapter.validateToken("  "))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid token")
                .extracting(ex -> ((InvalidTokenException) ex).getErrorCode())
                .isEqualTo(InvalidTokenException.INVALID_TOKEN);
    }
}
