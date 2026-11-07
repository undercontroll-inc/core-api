package com.undercontroll.application.controller;

import com.undercontroll.application.controller.doc.ApiResponseDocumentation.*;
import com.undercontroll.application.dto.auth.AuthUserRequest;
import com.undercontroll.application.dto.auth.AuthUserResponse;
import com.undercontroll.application.dto.auth.RefreshTokenRequest;
import com.undercontroll.application.dto.auth.RefreshTokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Auth", description = "Authentication and token management APIs")
@RequestMapping(value = "/v1/api/auth")
public interface AuthApi {

    @Operation(summary = "Sign in with a password or a Google account")
    @PostApiResponses
    @PostMapping
    ResponseEntity<AuthUserResponse> login(@Valid @RequestBody AuthUserRequest request);

    @Operation(summary = "Exchange a refresh token for a new access token")
    @PostApiResponses
    @PostMapping("/refresh")
    ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request);
}
