package com.undercontroll.domain.usecase.auth;

public interface RefreshTokenPort {

    Output execute(String refreshToken);

    record Output(String accessToken, String refreshToken) {
    }
}
