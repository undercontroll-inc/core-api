package com.undercontroll.infrastructure.web.controller;

import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undercontroll.application.dto.UserDto;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.model.enums.UserType;
import com.undercontroll.domain.port.in.*;
import com.undercontroll.domain.port.out.TokenPort;
import com.undercontroll.infrastructure.config.SecurityConfig;
import com.undercontroll.infrastructure.config.RateLimitProperties;
import com.undercontroll.infrastructure.web.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

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
    private AuthUserPort authUserPort;

    @MockitoBean
    private UpdateUserPort updateUserPort;

    @MockitoBean
    private GetUsersPort getUsersPort;

    @MockitoBean
    private GetCustomersPort getCustomersPort;

    @MockitoBean
    private GetCustomerByIdPort getCustomerByIdPort;

    @MockitoBean
    private GetCustomersWithEmailPort getCustomersWithEmailPort;

    @MockitoBean
    private GetUserPort getUserPort;

    @MockitoBean
    private DeleteUserPort deleteUserPort;

    @MockitoBean
    private ResetPasswordPort resetPasswordPort;

    // Required because AuthContextFilter depends on TokenPort
    @MockitoBean
    private TokenPort tokenPort;

    @MockitoBean
    private com.undercontroll.domain.port.in.RefreshTokenPort refreshTokenPort;

    private void mockTokenPortWithRole(String role) {
        Claim claim = mock(Claim.class);
        when(claim.asString()).thenReturn(role);
        DecodedJWT decoded = mock(DecodedJWT.class);
        when(decoded.getSubject()).thenReturn("user@example.com");
        when(decoded.getClaim("roles")).thenReturn(claim);
        when(tokenPort.validateToken(anyString())).thenReturn(decoded);
    }

    @Test
    @DisplayName("POST /v1/api/users - Should create user and return 201")
    void shouldCreateUserSuccessfully() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "John", "john@example.com", "11999999999", "Doe", "password123",
                "Street 123", "12345678900", null, UserType.CUSTOMER,
                false, false, true, "12345-678"
        );

        CreateUserPort.Output output = new CreateUserPort.Output(
                "John", "john@example.com", "Doe", "Street 123",
                "12345678900", "12345-678", "11999999999", null, UserType.CUSTOMER
        );

        when(createUserPort.execute(any(CreateUserPort.Input.class))).thenReturn(output);

        mockMvc.perform(post("/v1/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.userType").value("CUSTOMER"));

        verify(createUserPort, times(1)).execute(any(CreateUserPort.Input.class));
    }

    @Test
    @DisplayName("POST /v1/api/users/auth - Should authenticate user and return 200 with token")
    void shouldAuthenticateUserSuccessfully() throws Exception {
        AuthUserRequest request = new AuthUserRequest("john@example.com", "password123");

        UserDto userDto = new UserDto(1, "John", "john@example.com", "Doe",
                "Street 123", "12345678900", "12345-678", "11999999999",
                null, false, false, true, UserType.CUSTOMER);

        when(authUserPort.execute(any(AuthUserPort.Input.class)))
                .thenReturn(new AuthUserPort.Output("jwt-token-here", "refresh-token-here", userDto));

        mockMvc.perform(post("/v1/api/users/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-here"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-here"))
                .andExpect(jsonPath("$.user.email").value("john@example.com"));

        verify(authUserPort, times(1)).execute(any(AuthUserPort.Input.class));
    }

    @Test
    @DisplayName("POST /v1/api/users/auth/google - Should authenticate with Google and return 200")
    void shouldAuthenticateWithGoogleSuccessfully() throws Exception {
        AuthGoogleRequest request = new AuthGoogleRequest("john@example.com", "google-token");

        UserDto userDto = new UserDto(1, "John", "john@example.com", "Doe",
                "Street 123", "12345678900", "12345-678", "11999999999",
                null, false, false, true, UserType.CUSTOMER);

        when(authUserPort.execute(any(AuthUserPort.Input.class)))
                .thenReturn(new AuthUserPort.Output("jwt-token-here", "refresh-token-here", userDto));

        mockMvc.perform(post("/v1/api/users/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-here"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-here"))
                .andExpect(jsonPath("$.user.email").value("john@example.com"));

        verify(authUserPort, times(1)).execute(any(AuthUserPort.Input.class));
    }

    @Test
    @DisplayName("PUT /v1/api/users/{userId} - CUSTOMER should update user and return 200")
    void customerShouldUpdateUserSuccessfully() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest(
                "John Updated", "Doe", null, "New Address", null,
                "11999999999", null, "12345-678", false, false, true, UserType.CUSTOMER
        );

        when(updateUserPort.execute(any(UpdateUserPort.Input.class)))
                .thenReturn(new UpdateUserPort.Output(true, "Updated"));

        mockMvc.perform(put("/v1/api/users/1")
                        .with(user("customer@example.com").roles("SCOPE_CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(updateUserPort, times(1)).execute(any(UpdateUserPort.Input.class));
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

        when(getUsersPort.execute(any(GetUsersPort.Input.class)))
                .thenReturn(new GetUsersPort.Output(List.of(user1, user2)));

        mockMvc.perform(get("/v1/api/users")
                        .with(user("admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("John"))
                .andExpect(jsonPath("$[1].name").value("Jane"))
                .andExpect(jsonPath("$.length()").value(2));

        verify(getUsersPort, times(1)).execute(any(GetUsersPort.Input.class));
    }

    @Test
    @DisplayName("GET /v1/api/users - CUSTOMER should be forbidden and return 403")
    void customerShouldBeForbiddenToGetAllUsers() throws Exception {
        mockMvc.perform(get("/v1/api/users")
                        .with(user("customer@example.com").roles("SCOPE_CUSTOMER")))
                .andExpect(status().isForbidden());

        verify(getUsersPort, never()).execute(any(GetUsersPort.Input.class));
    }

    @Test
    @DisplayName("GET /v1/api/users/customers - ADMINISTRATOR should get customers and return 200")
    void administratorShouldGetCustomersSuccessfully() throws Exception {
        UserDto customer = new UserDto(1, "John", "john@example.com", "Doe",
                "Street 123", "12345678900", "12345-678", "11999999999",
                null, false, false, true, UserType.CUSTOMER);

        when(getCustomersPort.execute(any(GetCustomersPort.Input.class)))
                .thenReturn(new GetCustomersPort.Output(List.of(customer)));

        mockMvc.perform(get("/v1/api/users/customers")
                        .with(user("admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userType").value("CUSTOMER"));

        verify(getCustomersPort, times(1)).execute(any(GetCustomersPort.Input.class));
    }

    @Test
    @DisplayName("GET /v1/api/users/customers - Should return 204 when no customers found")
    void shouldReturn204WhenNoCustomersFound() throws Exception {
        when(getCustomersPort.execute(any(GetCustomersPort.Input.class)))
                .thenReturn(new GetCustomersPort.Output(List.of()));

        mockMvc.perform(get("/v1/api/users/customers")
                        .with(user("admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isNoContent());

        verify(getCustomersPort, times(1)).execute(any(GetCustomersPort.Input.class));
    }

    @Test
    @DisplayName("GET /v1/api/users/customers/{customerId} - Should get customer by id and return 200")
    void shouldGetCustomerByIdSuccessfully() throws Exception {
        UserDto customer = new UserDto(1, "John", "john@example.com", "Doe",
                "Street 123", "12345678900", "12345-678", "11999999999",
                null, false, false, true, UserType.CUSTOMER);

        when(getCustomerByIdPort.execute(any(GetCustomerByIdPort.Input.class)))
                .thenReturn(new GetCustomerByIdPort.Output(customer));

        mockMvc.perform(get("/v1/api/users/customers/1")
                        .with(user("admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John"));

        verify(getCustomerByIdPort, times(1)).execute(any(GetCustomerByIdPort.Input.class));
    }

    @Test
    @DisplayName("GET /v1/api/users/{userId} - Should get user by id and return 200")
    void shouldGetUserByIdSuccessfully() throws Exception {
        User user = User.builder()
                .id(1)
                .name("John")
                .email("john@example.com")
                .lastName("Doe")
                .userType(UserType.CUSTOMER)
                .build();

        when(getUserPort.execute(any(GetUserPort.Input.class)))
                .thenReturn(new GetUserPort.Output(user));

        mockMvc.perform(get("/v1/api/users/1")
                        .with(user("admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John"));

        verify(getUserPort, times(1)).execute(any(GetUserPort.Input.class));
    }

    @Test
    @DisplayName("DELETE /v1/api/users/{userId} - ADMINISTRATOR should delete user and return 200")
    void administratorShouldDeleteUserSuccessfully() throws Exception {
        when(deleteUserPort.execute(any(DeleteUserPort.Input.class)))
                .thenReturn(new DeleteUserPort.Output(true, "Deleted"));

        mockMvc.perform(delete("/v1/api/users/1")
                        .with(user("admin@example.com").roles("ADMINISTRATOR")))
                .andExpect(status().isOk());

        verify(deleteUserPort, times(1)).execute(any(DeleteUserPort.Input.class));
    }

    @Test
    @DisplayName("DELETE /v1/api/users/{userId} - CUSTOMER should be forbidden and return 403")
    void customerShouldBeForbiddenToDeleteUser() throws Exception {
        mockMvc.perform(delete("/v1/api/users/1")
                        .with(user("customer@example.com").roles("SCOPE_CUSTOMER")))
                .andExpect(status().isForbidden());

        verify(deleteUserPort, never()).execute(any(DeleteUserPort.Input.class));
    }

    @Test
    @DisplayName("PATCH /v1/api/users/reset-password/{userId} - CUSTOMER should reset password successfully and return 200")
    void shouldResetPasswordSuccessfully() throws Exception {
        mockTokenPortWithRole("SCOPE_CUSTOMER");

        ResetPasswordRequest request = new ResetPasswordRequest("newPassword123", false);

        when(resetPasswordPort.execute(any(ResetPasswordPort.Input.class)))
                .thenReturn(new ResetPasswordPort.Output(true, "Password reset"));

        mockMvc.perform(patch("/v1/api/users/reset-password/1")
                        .header("Authorization", "Bearer mock-customer-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(resetPasswordPort, times(1)).execute(any(ResetPasswordPort.Input.class));
    }

    @Test
    @DisplayName("PATCH /v1/api/users/reset-password/{userId} - ADMINISTRATOR should reset password successfully and return 200")
    void administratorShouldResetPasswordSuccessfully() throws Exception {
        mockTokenPortWithRole("SCOPE_ADMINISTRATOR");

        ResetPasswordRequest request = new ResetPasswordRequest("newAdminPassword123", true);

        when(resetPasswordPort.execute(any(ResetPasswordPort.Input.class)))
                .thenReturn(new ResetPasswordPort.Output(true, "Password reset"));

        mockMvc.perform(patch("/v1/api/users/reset-password/2")
                        .header("Authorization", "Bearer mock-admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(resetPasswordPort, times(1)).execute(any(ResetPasswordPort.Input.class));
    }
}
