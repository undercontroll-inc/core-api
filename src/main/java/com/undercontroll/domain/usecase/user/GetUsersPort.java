package com.undercontroll.domain.usecase.user;

import com.undercontroll.domain.enums.UserType;

import java.util.List;

public interface GetUsersPort {
    List<UserOutput> execute(UserType type, Boolean hasEmail);
}
