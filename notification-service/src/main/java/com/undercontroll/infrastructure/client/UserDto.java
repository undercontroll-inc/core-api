package com.undercontroll.infrastructure.client;

import java.util.UUID;

public record UserDto(
        Integer id,
        String name,
        String email,
        String lastName,
        String address,
        String cpf,
        String CEP,
        String phone,
        String avatarUrl,
        Boolean hasWhatsApp,
        Boolean alreadyRecurrent,
        Boolean inFirstLogin,
        String userType
) {}