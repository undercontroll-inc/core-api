package com.undercontroll.application.usecase.auth.impl;

import com.undercontroll.application.usecase.auth.AuthUserPort;
import com.undercontroll.domain.exception.InvalidAuthException;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.repository.UserRepositoryPort;
import com.undercontroll.application.port.TokenPort;
import com.undercontroll.application.port.RefreshTokenPort;
import com.undercontroll.application.port.MetricsPort;
import com.undercontroll.application.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthUserImpl implements AuthUserPort {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final TokenPort tokenPort;
    private final RefreshTokenPort refreshTokenPort;
    private final MetricsPort metricsPort;

    @Override
    public Output execute(Input input) {
        try {
            Optional<User> userFound = userRepositoryPort.findByEmail(input.email());

            if (userFound.isEmpty()) {
                metricsPort.incrementLoginFailed();
                throw new InvalidAuthException("Email or password is invalid");
            }

            User user = userFound.get();

            // Google auth passes null password — skip password check
            if (input.password() != null) {
                boolean passwordMatch = passwordEncoder.matches(input.password(), user.getPassword());
                if (!passwordMatch) {
                    metricsPort.incrementLoginFailed();
                    throw new InvalidAuthException("Email or password is invalid");
                }
            }

            String accessToken = tokenPort.generateToken(String.valueOf(user.getId()), user.getUserType());
            String refreshToken = refreshTokenPort.createRefreshToken(user.getId(), user.getEmail(), user.getUserType());

            metricsPort.incrementLoginSuccess();

            return new Output(accessToken, refreshToken, mapToDto(user));
        } catch (InvalidAuthException e) {
            throw e;
        }
    }

    private UserDto mapToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getLastName(),
                user.getAddress(),
                user.getCpf(),
                user.getCEP(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getHasWhatsApp(),
                user.getAlreadyRecurrent(),
                user.getInFirstLogin(),
                user.getUserType()
        );
    }
}
