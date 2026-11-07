package com.undercontroll.domain.usecase.auth;

public interface ResetPasswordPort {

    void execute(Integer userId, Input input, String token);

    record Input(String newPassword, boolean inFirstLogin) {
    }
}
