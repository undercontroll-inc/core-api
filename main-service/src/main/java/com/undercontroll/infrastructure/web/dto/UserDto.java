package com.undercontroll.infrastructure.web.dto;

import com.undercontroll.domain.entity.enums.UserType;

public record UserDto (
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
) {}
