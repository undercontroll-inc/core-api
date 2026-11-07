package com.undercontroll.domain.usecase.user.impl;

import com.undercontroll.domain.usecase.user.GetUsersPort;
import com.undercontroll.domain.usecase.user.UserOutput;
import com.undercontroll.domain.enums.UserType;
import com.undercontroll.domain.gateway.UserGateway;
import com.undercontroll.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetUsersImpl implements GetUsersPort {

    static final int LIST_LIMIT = 500;

    private final UserGateway userGateway;

    @Override
    public List<UserOutput> execute(UserType type, Boolean hasEmail) {
        List<User> users;

        if (UserType.CUSTOMER.equals(type)) {
            users = Boolean.TRUE.equals(hasEmail)
                    ? userGateway.findCustomersWithEmail(LIST_LIMIT)
                    : userGateway.findCustomers(LIST_LIMIT);
        } else {
            users = userGateway.findAll(LIST_LIMIT);
        }

        return users.stream()
                .map(UserOutput::from)
                .toList();
    }
}
