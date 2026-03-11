package com.undercontroll.application.usecase.auth.impl;

import com.undercontroll.application.usecase.auth.RefreshTokenPort;
import com.undercontroll.domain.enums.UserType;
import com.undercontroll.application.port.RefreshTokenPort.RefreshTokenData;
import com.undercontroll.application.port.TokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenImpl implements RefreshTokenPort {

    private final com.undercontroll.application.port.RefreshTokenPort refreshTokenPort;
    private final TokenPort tokenPort;

    @Override
    public Output execute(Input input) {
        RefreshTokenData data = refreshTokenPort.validateRefreshToken(input.refreshToken());

        UserType userType = UserType.valueOf(data.userRole());

        // Rotate: revoke old token and issue a new refresh token
        String newRefreshToken = refreshTokenPort.createRefreshToken(
                data.userId(), data.userEmail(), userType
        );

        String newAccessToken = tokenPort.generateToken(String.valueOf(data.userId()), userType);

        return new Output(newAccessToken, newRefreshToken);
    }
}

