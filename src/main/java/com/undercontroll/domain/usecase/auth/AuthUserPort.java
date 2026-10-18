package com.undercontroll.domain.usecase.auth;

import com.undercontroll.domain.enums.AuthProvider;
import com.undercontroll.domain.usecase.user.UserOutput;

public interface AuthUserPort {

    Output execute(Input input);

    record Input(AuthProvider provider, String email, String password, String token) {
    }

    record Output(String accessToken, String refreshToken, UserOutput user) {
    }
}
