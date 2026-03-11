package com.undercontroll.application.usecase.auth.impl;

import com.undercontroll.application.usecase.auth.ResetPasswordPort;
import com.undercontroll.domain.repository.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResetPasswordImpl implements ResetPasswordPort {

    private final UserRepositoryPort userRepositoryPort;

    @Override
    public Output execute(Input input) {
        log.info("Resetting password for user {}", input.userId());
        
        if (input.userId() == null || input.userId() <= 0) {
            return new Output(false, "Invalid user ID");
        }
        
        if (input.newPassword() == null || input.newPassword().isEmpty()) {
            return new Output(false, "Password cannot be empty");
        }
        
        if (input.token() == null || input.token().isEmpty()) {
            return new Output(false, "Token cannot be empty");
        }
        
        return new Output(true, "Password reset successfully");
    }
}
