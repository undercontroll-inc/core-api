package com.undercontroll.domain.port.in;

import com.undercontroll.domain.entity.enums.UserType;
import com.undercontroll.infrastructure.web.dto.CreateUserResponse;

public interface CreateUserPort {
    record Input(
            String name,
            String email,
            String phone,
            String lastName,
            String password,
            String address,
            String cpf,
            String avatarUrl,
            UserType userType,
            Boolean hasWhatsApp,
            Boolean alreadyRecurrent,
            Boolean inFirstLogin,
            String CEP
    ) {}

    record Output(
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

    Output execute(Input input);
}
