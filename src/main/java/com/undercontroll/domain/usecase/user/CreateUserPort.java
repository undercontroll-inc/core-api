package com.undercontroll.domain.usecase.user;

import com.undercontroll.domain.enums.UserType;

public interface CreateUserPort {

    Output execute(Input input);

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
    ) {
    }

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
    ) {
    }
}
