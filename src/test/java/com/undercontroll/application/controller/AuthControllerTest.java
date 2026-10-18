package com.undercontroll.application.controller;

import com.undercontroll.domain.usecase.user.UserOutput;
import com.undercontroll.application.mapper.AuthDtoMapperImpl;
import com.undercontroll.application.mapper.UserDtoMapperImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.application.controller.impl.AuthController;
import com.undercontroll.application.dto.auth.AuthUserRequest;
import com.undercontroll.application.dto.auth.RefreshTokenRequest;
import com.undercontroll.domain.enums.AuthProvider;
import com.undercontroll.domain.enums.UserType;
import com.undercontroll.domain.exception.InvalidTokenException;
import com.undercontroll.domain.usecase.auth.AuthUserPort;
import com.undercontroll.domain.usecase.auth.RefreshTokenPort;
import com.undercontroll.infrastructure.config.RateLimitProperties;
import com.undercontroll.infrastructure.config.SecurityConfig;
import com.undercontroll.infrastructure.handler.GlobalExceptionHandler;
import com.undercontroll.domain.gateway.TokenGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, RateLimitProperties.class, GlobalExceptionHandler.class, AuthDtoMapperImpl.class, UserDtoMapperImpl.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthUserPort authUserPort;

    @MockitoBean
    private RefreshTokenPort refreshTokenPort;

    @MockitoBean
    private TokenGateway tokenGateway;

    @Test
    @DisplayName("POST /v1/api/auth - Should authenticate with password and return 200 with token")
    void shouldAuthenticateUserSuccessfully() throws Exception {
        AuthUserRequest request = new AuthUserRequest(
                AuthProvider.PASSWORD, "john@example.com", "password123", null);

        UserOutput userDto = new UserOutput(1, "John", "john@example.com", "Doe",
                "Street 123", "12345678900", "12345-678", "11999999999",
                null, false, false, true, UserType.CUSTOMER);

        when(authUserPort.execute(any(AuthUserPort.Input.class)))
                .thenReturn(new AuthUserPort.Output("jwt-token-here", "refresh-token-here", userDto));

        mockMvc.perform(post("/v1/api/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token-here"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-here"))
                .andExpect(jsonPath("$.user.email").value("john@example.com"));

        verify(authUserPort, times(1)).execute(any(AuthUserPort.Input.class));
    }

    @Test
    @DisplayName("POST /v1/api/auth - Should authenticate with Google and return 200")
    void shouldAuthenticateWithGoogleSuccessfully() throws Exception {
        AuthUserRequest request = new AuthUserRequest(
                AuthProvider.GOOGLE, "john@example.com", null, "google-token");

        UserOutput userDto = new UserOutput(1, "John", "john@example.com", "Doe",
                "Street 123", "12345678900", "12345-678", "11999999999",
                null, false, false, true, UserType.CUSTOMER);

        when(authUserPort.execute(any(AuthUserPort.Input.class)))
                .thenReturn(new AuthUserPort.Output("jwt-token-here", "refresh-token-here", userDto));

        mockMvc.perform(post("/v1/api/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token-here"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-here"))
                .andExpect(jsonPath("$.user.email").value("john@example.com"));

        verify(authUserPort, times(1)).execute(any(AuthUserPort.Input.class));
    }

    @Test
    @DisplayName("POST /v1/api/auth/refresh - Should exchange refresh token and return 200")
    void shouldRefreshTokenSuccessfully() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("old-refresh-token");

        when(refreshTokenPort.execute(anyString()))
                .thenReturn(new RefreshTokenPort.Output("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/v1/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));

        verify(refreshTokenPort, times(1)).execute("old-refresh-token");
    }

    @Test
    @DisplayName("POST /v1/api/auth/refresh - Expired access token on the request must not block refresh")
    void shouldRefreshEvenWhenExpiredAccessTokenIsSent() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("old-refresh-token");

        when(tokenGateway.validateToken(any()))
                .thenThrow(new InvalidTokenException("Access token has expired", InvalidTokenException.TOKEN_EXPIRED));
        when(refreshTokenPort.execute(anyString()))
                .thenReturn(new RefreshTokenPort.Output("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/v1/api/auth/refresh")
                        .header("Authorization", "Bearer expired-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));

        verify(refreshTokenPort, times(1)).execute("old-refresh-token");
    }

    @Test
    @DisplayName("POST /v1/api/auth/refresh - Blank refresh token should return 400")
    void shouldRejectBlankRefreshToken() throws Exception {
        mockMvc.perform(post("/v1/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(refreshTokenPort);
    }
}
