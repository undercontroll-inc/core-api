package com.undercontroll.domain.usecase.user;

import com.undercontroll.domain.enums.UserType;

public interface UpdateUserPort {

    void execute(Integer userId, Input input);

    record Input(
            String name,
            String lastName,
            String password,
            String address,
            String cpf,
            String phone,
            String avatarUrl,
            String CEP,
            Boolean alreadyRecurrent,
            Boolean hasWhatsApp,
            Boolean inFirstLogin,
            UserType userType
    ) {
    }
}
