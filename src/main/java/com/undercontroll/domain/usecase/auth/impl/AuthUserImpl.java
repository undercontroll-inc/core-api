package com.undercontroll.domain.usecase.auth.impl;

import com.undercontroll.application.dto.auth.AuthUserRequest;
import com.undercontroll.application.dto.auth.AuthUserResponse;
import com.undercontroll.application.mapper.UserDtoMapper;
import com.undercontroll.domain.enums.AuthProvider;
import com.undercontroll.domain.exception.InvalidAuthException;
import com.undercontroll.domain.gateway.UserGateway;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.usecase.auth.AuthUserPort;
import com.undercontroll.domain.gateway.GoogleAuthGateway;
import com.undercontroll.domain.gateway.MetricsGateway;
import com.undercontroll.domain.gateway.RefreshTokenGateway;
import com.undercontroll.domain.gateway.TokenGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthUserImpl implements AuthUserPort {

    private final UserGateway userGateway;
    private final PasswordEncoder passwordEncoder;
    private final TokenGateway tokenGateway;
    private final RefreshTokenGateway refreshTokenGateway;
    private final MetricsGateway metricsGateway;
    private final UserDtoMapper userDtoMapper;
    private final GoogleAuthGateway googleAuthGateway;

    @Override
    public AuthUserResponse execute(AuthUserRequest request) {
        Optional<User> userFound = userGateway.findByEmail(request.email());
        if (userFound.isEmpty()) {
            return fail();
        }

        User user = userFound.get();
        if (request.provider() == null || !isAuthenticated(request, user)) {
            return fail();
        }

        String accessToken = tokenGateway.generateToken(String.valueOf(user.getId()), user.getUserType());
        String refreshToken = refreshTokenGateway.createRefreshToken(user.getId(), user.getEmail(), user.getUserType());
        metricsGateway.incrementLoginSuccess();
        log.info("Login succeeded userId={} provider={}", user.getId(), request.provider());
        return new AuthUserResponse(accessToken, refreshToken, userDtoMapper.toDto(user));
    }

    private boolean isAuthenticated(AuthUserRequest request, User user) {
        return switch (request.provider()) {
            case PASSWORD -> request.password() != null
                    && !request.password().isBlank()
                    && passwordEncoder.matches(request.password(), user.getPassword());
            case GOOGLE -> request.token() != null
                    && !request.token().isBlank()
                    && googleAuthGateway.verify(request.token(), request.email());
        };
    }

    private AuthUserResponse fail() {
        metricsGateway.incrementLoginFailed();
        log.warn("Login failed");
        throw new InvalidAuthException("Email or password is invalid");
    }
}
