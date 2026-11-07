package com.undercontroll.domain.usecase.user.impl;

import com.undercontroll.domain.usecase.user.GetUserPort;
import com.undercontroll.domain.usecase.user.UserOutput;
import com.undercontroll.domain.exception.InvalidUserException;
import com.undercontroll.domain.gateway.UserGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetUserImpl implements GetUserPort {

    private final UserGateway userGateway;

    @Override
    @Cacheable(value = "user", key = "#userId")
    public Optional<UserOutput> execute(Integer userId) {
        if (userId == null) {
            throw new InvalidUserException("User ID cannot be null");
        }

        return userGateway.findById(userId).map(UserOutput::from);
    }
}
