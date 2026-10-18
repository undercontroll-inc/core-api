package com.undercontroll.domain.usecase.auth;

import com.undercontroll.domain.enums.PasswordEventType;
import com.undercontroll.domain.model.PasswordEvent;

public interface CreatePasswordEventPort {

    PasswordEvent execute(Input input);

    record Input(PasswordEventType type, String agent, String userPhone, String value) {
    }
}
