package com.undercontroll.infrastructure.web.dto;

import com.undercontroll.domain.model.enums.UserType;

public record CreateUserResponse(
        String name,
        String email,
        String lastName,
        String address,
        String cpf,
        String CEP,
        String phone,
        String avatarUrl,
        UserType userType
) {}
