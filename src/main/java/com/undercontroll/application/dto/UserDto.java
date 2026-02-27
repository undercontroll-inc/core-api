package com.undercontroll.application.dto;

import com.undercontroll.domain.model.enums.UserType;

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
        UserType userType
) {
}
