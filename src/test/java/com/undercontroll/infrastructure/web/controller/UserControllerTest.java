package com.undercontroll.infrastructure.web.controller;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.application.dto.user.CreateUserRequest;
import com.undercontroll.application.dto.user.CreateUserResponse;
import com.undercontroll.application.dto.auth.ResetPasswordRequest;
import com.undercontroll.application.dto.user.UpdateUserRequest;
import com.undercontroll.application.dto.user.UserDto;
import com.undercontroll.domain.usecase.auth.ResetPasswordPort;
import com.undercontroll.domain.enums.UserType;
import com.undercontroll.domain.exception.InvalidTokenException;
import com.undercontroll.domain.gateway.TokenGateway;
import com.undercontroll.domain.usecase.user.*;
import com.undercontroll.infrastructure.config.SecurityConfig;
import com.undercontroll.infrastructure.config.RateLimitProperties;
import com.undercontroll.application.controller.impl.UserController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, RateLimitProperties.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateUserPort createUserPort;

    @MockitoBean
    private UpdateUserPort updateUserPort;

    @MockitoBean
    private GetUsersPort getUsersPort;

    @MockitoBean
    private GetUserPort getUserPort;

    @MockitoBean
    private DeleteUserPort deleteUserPort;

    @MockitoBean
    private ResetPasswordPort resetPasswordPort;

    // Required because AuthContextFilter depends on TokenPort
    @MockitoBean
    private TokenGateway tokenGateway;

    private void mockTokenPortWithRole(String role) {
        Claim claim = mock(Claim.class);
        when(claim.asString()).thenReturn(role);
        DecodedJWT decoded = mock(DecodedJWT.class);
        when(decoded.getSubject()).thenReturn("user@example.com");
        when(decoded.getClaim("roles")).thenReturn(claim);
        when(tokenGateway.validateToken(anyString())).thenReturn(decoded);
    }

    @Test
    @DisplayName("POST /v1/api/users - Should create user and return 201")
    void shouldCreateUserSuccessfully() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "John", "john@example.com", "11999999999", "Doe", "password123",
                "Street 123", "12345678900", null, UserType.CUSTOMER,
                false, false, true, "12345-678"
        );

        CreateUserResponse response = new CreateUserResponse(
                "John", "john@example.com", "Doe", "Street 123",
                "12345678900", "12345-678", "11999999999", null, UserType.CUSTOMER
        );

        when(createUserPort.execute(any(CreateUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/v1/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.userType").value("CUSTOMER"));

        verify(createUserPort, times(1)).execute(any(CreateUserRequest.class));
    }

    @Test
    @DisplayName("PATCH /v1/api/users/{userId} - CUSTOMER should update user and return 200")
    void customerShouldUpdateUserSuccessfully() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest(
                "John Updated", "Doe", null, "New Address", null,
                "11999999999", null, "12345-678", false, false, true, UserType.CUSTOMER
        );

        mockMvc.perform(patch("/v1/api/users/1")
                        .with(user("customer@example.com").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(updateUserPort, times(1)).execute(eq(1), any(UpdateUserRequest.class));
    }

    @Test
    @DisplayName("GET /v1/api/users - ADMINISTRATOR should get all users and return 200")
    void administratorShouldGetAllUsersSuccessfully() throws Exception {
        UserDto user1 = new UserDto(1, "John", "john@example.com", "Doe",
                "Street 123", "12345678900", "12345-678", "11999999999",
                null, false, false, true, UserType.CUSTOMER);

        UserDto user2 = new UserDto(2, "Jane", "jane@example.com", "Doe",
                "Street 456", "98765432100", "54321-987", "11988888888",
                null, false, false, true, UserType.ADMINISTRATOR);

        when(getUsersPort.execute(null, null)).thenReturn(List.of(user1, user2));

        mockMvc.perform(get("/v1/api/users")
                        .with(user("admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("John"))
                .andExpect(jsonPath("$[1].name").value("Jane"))
                .andExpect(jsonPath("$.length()").value(2));

        verify(getUsersPort, times(1)).execute(null, null);
    }

    @Test
    @DisplayName("GET /v1/api/users - CUSTOMER should be forbidden and return 403")
    void customerShouldBeForbiddenToGetAllUsers() throws Exception {
        mockMvc.perform(get("/v1/api/users")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isForbidden());

        verify(getUsersPort, never()).execute(any(), any());
    }

    @Test
    @DisplayName("GET /v1/api/users?type=CUSTOMER - ADMINISTRATOR should get customers and return 200")
    void administratorShouldGetCustomersSuccessfully() throws Exception {
        UserDto customer = new UserDto(1, "John", "john@example.com", "Doe",
                "Street 123", "12345678900", "12345-678", "11999999999",
                null, false, false, true, UserType.CUSTOMER);

        when(getUsersPort.execute(UserType.CUSTOMER, null)).thenReturn(List.of(customer));

        mockMvc.perform(get("/v1/api/users").param("type", "CUSTOMER")
                        .with(user("admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userType").value("CUSTOMER"));

        verify(getUsersPort, times(1)).execute(UserType.CUSTOMER, null);
    }

    @Test
    @DisplayName("GET /v1/api/users?type=CUSTOMER - Should return 200 with an empty list when no customers found")
    void shouldReturnEmptyListWhenNoCustomersFound() throws Exception {
        when(getUsersPort.execute(UserType.CUSTOMER, null)).thenReturn(List.of());

        mockMvc.perform(get("/v1/api/users").param("type", "CUSTOMER")
                        .with(user("admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(getUsersPort, times(1)).execute(UserType.CUSTOMER, null);
    }

    @Test
    @DisplayName("GET /v1/api/users?type=CUSTOMER&hasEmail=true - ADMINISTRATOR should get customers with email and return 200")
    void administratorShouldGetCustomersWithEmailSuccessfully() throws Exception {
        UserDto customer = new UserDto(1, "John", "john@example.com", "Doe",
                "Street 123", "12345678900", "12345-678", "11999999999",
                null, false, false, true, UserType.CUSTOMER);

        when(getUsersPort.execute(UserType.CUSTOMER, true)).thenReturn(List.of(customer));

        mockMvc.perform(get("/v1/api/users").param("type", "CUSTOMER").param("hasEmail", "true")
                        .with(user("admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("john@example.com"));

        verify(getUsersPort, times(1)).execute(UserType.CUSTOMER, true);
    }

    @Test
    @DisplayName("GET /v1/api/users/{userId} - Should get user by id and return 200 (also covers former customer-by-id lookup)")
    void shouldGetUserByIdSuccessfully() throws Exception {
        UserDto userDto = new UserDto(1, "John", "john@example.com", "Doe",
                null, null, null, null, null, null, null, null, UserType.CUSTOMER);

        when(getUserPort.execute(1)).thenReturn(Optional.of(userDto));

        mockMvc.perform(get("/v1/api/users/1")
                        .with(user("admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John"));

        verify(getUserPort, times(1)).execute(1);
    }

    @Test
    @DisplayName("DELETE /v1/api/users/{userId} - ADMINISTRATOR should delete user and return 204")
    void administratorShouldDeleteUserSuccessfully() throws Exception {
        mockMvc.perform(delete("/v1/api/users/1")
                        .with(user("admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isNoContent());

        verify(deleteUserPort, times(1)).execute(1);
    }

    @Test
    @DisplayName("DELETE /v1/api/users/{userId} - CUSTOMER should be forbidden and return 403")
    void customerShouldBeForbiddenToDeleteUser() throws Exception {
        mockMvc.perform(delete("/v1/api/users/1")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isForbidden());

        verify(deleteUserPort, never()).execute(anyInt());
    }

    @Test
    @DisplayName("PATCH /v1/api/users/{userId}/password - CUSTOMER should change password successfully and return 200")
    void shouldResetPasswordSuccessfully() throws Exception {
        mockTokenPortWithRole("CUSTOMER");

        ResetPasswordRequest request = new ResetPasswordRequest("newPassword123", false);

        mockMvc.perform(patch("/v1/api/users/1/password")
                        .header("Authorization", "Bearer mock-customer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(resetPasswordPort, times(1)).execute(eq(1), any(ResetPasswordRequest.class), eq("mock-customer-token"));
    }

    @Test
    @DisplayName("PATCH /v1/api/users/{userId}/password - ADMINISTRATOR should change password successfully and return 200")
    void administratorShouldResetPasswordSuccessfully() throws Exception {
        mockTokenPortWithRole("ADMINISTRATOR");

        ResetPasswordRequest request = new ResetPasswordRequest("newAdminPassword123", true);

        mockMvc.perform(patch("/v1/api/users/2/password")
                        .header("Authorization", "Bearer mock-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(resetPasswordPort, times(1)).execute(eq(2), any(ResetPasswordRequest.class), eq("mock-admin-token"));
    }

    @Test
    @DisplayName("GET /v1/api/users - Expired JWT should return 401 with TOKEN_EXPIRED")
    void expiredJwtShouldReturn401WithTokenExpiredCode() throws Exception {
        when(tokenGateway.validateToken(anyString()))
                .thenThrow(new InvalidTokenException("Access token has expired", InvalidTokenException.TOKEN_EXPIRED));

        mockMvc.perform(get("/v1/api/users")
                        .header("Authorization", "Bearer expired-access-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"))
                .andExpect(jsonPath("$.message").value("Access token has expired"));

        verify(getUsersPort, never()).execute(any(), any());
    }
}
