package com.undercontroll.domain.usecase.user;

import java.util.Optional;

public interface GetUserPort {
    Optional<UserOutput> execute(Integer userId);
}
