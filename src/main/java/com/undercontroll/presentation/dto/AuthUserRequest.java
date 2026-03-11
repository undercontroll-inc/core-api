package com.undercontroll.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Requisição de autenticação de usuário")
public record AuthUserRequest(

        @Schema(
                description = "Email do usuário",
                example = "admin@undercontroll.com",
                format = "email"
        )
        @NotBlank
        @Email
        String email,

        @Schema(
                description = "Senha do usuário",
                example = "SenhaSegura123!",
                format = "password"
        )
        @NotBlank
        String password
) {
}
