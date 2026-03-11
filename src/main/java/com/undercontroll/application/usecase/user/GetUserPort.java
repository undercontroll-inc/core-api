package com.undercontroll.application.usecase.user;

import com.undercontroll.domain.model.User;

public interface GetUserPort {
    record Input(
            Integer userId
    ) {}

    record Output(
            User user
    ) {}

    Output execute(Input input);
}
