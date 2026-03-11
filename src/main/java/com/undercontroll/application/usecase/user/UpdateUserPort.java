package com.undercontroll.application.usecase.user;

import com.undercontroll.domain.enums.UserType;

public interface UpdateUserPort {
    record Input(
            Integer userId,
            String name,
            String lastName,
            String address,
            String cpf,
            String password,
            Boolean hasWhatsApp,
            String CEP,
            Boolean alreadyRecurrent,
            Boolean inFirstLogin,
            String phone,
            String avatarUrl,
            UserType userType
    ) {}

    record Output(
            Boolean success,
            String message
    ) {}

    Output execute(Input input);
}
