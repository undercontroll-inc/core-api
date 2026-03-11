package com.undercontroll.application.usecase.auth;

public interface RefreshTokenPort {

    record Input(String refreshToken) {}

    record Output(String accessToken, String refreshToken) {}

    Output execute(Input input);
}

