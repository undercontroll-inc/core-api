package com.undercontroll.application.controller.impl;

import com.undercontroll.application.controller.AuthApi;
import com.undercontroll.application.dto.auth.AuthUserRequest;
import com.undercontroll.application.dto.auth.AuthUserResponse;
import com.undercontroll.application.dto.auth.RefreshTokenRequest;
import com.undercontroll.application.dto.auth.RefreshTokenResponse;
import com.undercontroll.application.mapper.AuthDtoMapper;
import com.undercontroll.domain.usecase.auth.AuthUserPort;
import com.undercontroll.domain.usecase.auth.RefreshTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthUserPort authUserPort;
    private final RefreshTokenPort refreshTokenPort;
    private final AuthDtoMapper mapper;

    @Override
    public ResponseEntity<AuthUserResponse> login(AuthUserRequest request) {
        return ResponseEntity.ok(mapper.toResponse(authUserPort.execute(mapper.toInput(request))));
    }

    @Override
    public ResponseEntity<RefreshTokenResponse> refresh(RefreshTokenRequest request) {
        return ResponseEntity.ok(mapper.toResponse(refreshTokenPort.execute(request.refreshToken())));
    }
}
