package com.undercontroll.infrastructure.web.dto;

import com.undercontroll.domain.model.enums.UserType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Requisição para criação de novo usuário")
public record CreateUserRequest(
        @Schema(description = "Nome do usuário", example = "João")
        String name,

        @Schema(description = "Email do usuário (único)", example = "joao.silva@email.com", format = "email")
        String email,

        @Schema(description = "Telefone do usuário", example = "11987654321")
        String phone,

        @Schema(description = "Sobrenome do usuário", example = "Silva")
        String lastName,

        @Schema(description = "Senha do usuário", example = "SenhaSegura123!", format = "password")
        String password,

        @Schema(description = "Endereço completo", example = "Rua das Flores, 123 - São Paulo/SP")
        String address,

        @Schema(description = "CPF do usuário (apenas números)", example = "12345678900")
        String cpf,

        @Schema(description = "URL da foto de perfil", example = "https://example.com/avatar.jpg")
        String avatarUrl,

        @Schema(description = "Tipo de usuário", example = "CUSTOMER", allowableValues = {"CUSTOMER", "ADMINISTRATOR"})
        UserType userType,

        @Schema(description = "Indica se possui WhatsApp", example = "true")
        Boolean hasWhatsApp,

        @Schema(description = "Indica se é cliente recorrente", example = "false")
        Boolean alreadyRecurrent,

        @Schema(description = "Indica se está no primeiro login", example = "true")
        Boolean inFirstLogin,

        @Schema(description = "CEP do endereço", example = "01234-567")
        String CEP
){
}
