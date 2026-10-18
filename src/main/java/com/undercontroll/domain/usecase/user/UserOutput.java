package com.undercontroll.domain.usecase.user;

import com.undercontroll.domain.enums.UserType;
import com.undercontroll.domain.model.User;

public record UserOutput(
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

    public static UserOutput from(User user) {
        if (user == null) {
            return null;
        }
        return new UserOutput(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getLastName(),
                user.getAddress(),
                user.getCpf(),
                user.getCEP(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getHasWhatsApp(),
                user.getAlreadyRecurrent(),
                user.getInFirstLogin(),
                user.getUserType()
        );
    }
}
