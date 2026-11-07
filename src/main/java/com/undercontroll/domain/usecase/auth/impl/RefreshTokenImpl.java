package com.undercontroll.domain.usecase.auth.impl;

import com.undercontroll.domain.usecase.auth.RefreshTokenPort;
import com.undercontroll.domain.enums.UserType;
import com.undercontroll.domain.gateway.RefreshTokenGateway;
import com.undercontroll.domain.gateway.RefreshTokenGateway.RefreshTokenData;
import com.undercontroll.domain.gateway.TokenGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenImpl implements RefreshTokenPort {

    private final RefreshTokenGateway refreshTokenGateway;
    private final TokenGateway tokenGateway;

    @Override
    @Transactional
    public Output execute(String refreshToken) {
        RefreshTokenData data = refreshTokenGateway.consumeRefreshToken(refreshToken);

        UserType userType = UserType.valueOf(data.userRole());

        String newRefreshToken = refreshTokenGateway.createRefreshToken(
                data.userId(), data.userEmail(), userType
        );

        String newAccessToken = tokenGateway.generateToken(String.valueOf(data.userId()), userType);
        log.info("Refresh token rotated userId={}", data.userId());
        return new Output(newAccessToken, newRefreshToken);
    }
}
