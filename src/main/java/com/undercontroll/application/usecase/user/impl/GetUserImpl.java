package com.undercontroll.application.usecase.user.impl;

import com.undercontroll.application.usecase.user.GetUserPort;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.exception.InvalidUserException;
import com.undercontroll.domain.repository.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetUserImpl implements GetUserPort {

    private final UserRepositoryPort userRepositoryPort;

    @Override
    @Cacheable(value = "user", key = "#input.userId()")
    public Output execute(Input input) {
        if (input.userId() == null) {
            throw new InvalidUserException("User ID cannot be null");
        }

        Optional<User> user = userRepositoryPort.findById(input.userId());

        if (user.isEmpty()) {
            throw new InvalidUserException("Could not found the user with id: %d".formatted(input.userId()));
        }

        return new Output(user.get());
    }
}
