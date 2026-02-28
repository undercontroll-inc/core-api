package com.undercontroll.application.usecase;

import com.undercontroll.domain.model.enums.UserType;
import com.undercontroll.domain.port.in.RefreshTokenPort;
import com.undercontroll.domain.port.out.RefreshTokenPort.RefreshTokenData;
import com.undercontroll.domain.port.out.TokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefreshTokenImpl implements RefreshTokenPort {

    private final com.undercontroll.domain.port.out.RefreshTokenPort refreshTokenPort;
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

