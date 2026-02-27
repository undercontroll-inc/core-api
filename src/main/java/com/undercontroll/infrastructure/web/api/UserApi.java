package com.undercontroll.infrastructure.web.api;

import com.undercontroll.infrastructure.config.ApiResponseDocumentation.*;
import com.undercontroll.application.dto.*;
import com.undercontroll.infrastructure.web.dto.*;
import com.undercontroll.domain.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Users", description = "APIs para gerenciamento de usuários e autenticação")
public interface UserApi {

    @Operation(summary = "Criar novo usuário")
    @PostApiResponses
    ResponseEntity<CreateUserResponse> createUser(CreateUserRequest request);

    @Operation(summary = "Autenticar usuário")
    @PostApiResponses
    ResponseEntity<AuthUserResponse> auth(AuthUserRequest request);

    @Operation(summary = "Autenticar via Google")
    @PostApiResponses
    ResponseEntity<AuthUserResponse> authGoogle(AuthGoogleRequest request);

    @Operation(summary = "Atualizar usuário")
    @PutApiResponses
    @SecurityRequirement(name = "Bearer Authentication")
    ResponseEntity<Void> updateUser(UpdateUserRequest request, @Parameter(example = "1") Integer userId);

    @Operation(summary = "Listar todos os usuários")
    @GetApiResponses
    @SecurityRequirement(name = "Bearer Authentication")
    ResponseEntity<List<UserDto>> getUsers();

    @Operation(summary = "Listar todos os clientes")
    @GetApiResponses
    @SecurityRequirement(name = "Bearer Authentication")
    ResponseEntity<List<UserDto>> getCostumers();

    @Operation(summary = "Buscar cliente por ID")
    @GetApiResponses
    @SecurityRequirement(name = "Bearer Authentication")
    ResponseEntity<UserDto> getCostumerById(@Parameter(example = "1") Integer customerId);

    @Operation(summary = "Listar todos os clientes com e-mail")
    @SecurityRequirement(name = "Bearer Authentication")
    ResponseEntity<List<UserDto>> getCustomersThatHaveEmail();

    @Operation(summary = "Buscar usuário por ID")
    @GetApiResponses
    @SecurityRequirement(name = "Bearer Authentication")
    ResponseEntity<User> getUserById(@Parameter(example = "1") Integer userId);

    @Operation(summary = "Deletar usuário")
    @DeleteApiResponses
    @SecurityRequirement(name = "Bearer Authentication")
    ResponseEntity<Void> deleteUser(@Parameter(example = "1") Integer userId);

    @Operation(summary = "Resetar senha do usuário")
    @PutApiResponses
    @SecurityRequirement(name = "Bearer Authentication")
    ResponseEntity<Void> resetPassword(ResetPasswordRequest request, @Parameter(example = "1") Integer userId, String authHeader);
}

