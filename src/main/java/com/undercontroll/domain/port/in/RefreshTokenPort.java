package com.undercontroll.domain.port.in;

public interface RefreshTokenPort {

    record Input(String refreshToken) {}

    record Output(String accessToken, String refreshToken) {}

    Output execute(Input input);
}

