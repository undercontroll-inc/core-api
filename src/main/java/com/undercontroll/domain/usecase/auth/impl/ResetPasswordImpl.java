package com.undercontroll.domain.usecase.auth.impl;

import com.undercontroll.domain.exception.InvalidPasswordResetException;
import com.undercontroll.domain.usecase.auth.ResetPasswordPort;
import com.undercontroll.domain.gateway.UserGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResetPasswordImpl implements ResetPasswordPort {

    private final UserGateway userGateway;

    @Override
    public void execute(Integer userId, Input request, String token) {
        log.info("Resetting password for user {}", userId);

        if (userId == null || userId <= 0) {
            throw new InvalidPasswordResetException("Invalid user ID");
        }

        if (request.newPassword() == null || request.newPassword().isEmpty()) {
            throw new InvalidPasswordResetException("Password cannot be empty");
        }

        if (token == null || token.isEmpty()) {
            throw new InvalidPasswordResetException("Token cannot be empty");
        }
    }
}
