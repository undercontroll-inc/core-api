package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.DeleteUserPort;
import com.undercontroll.domain.exception.InvalidUserException;
import com.undercontroll.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteUserImpl implements DeleteUserPort {

    private final UserJpaRepository repository;

    @Override
    @CacheEvict(value = {"users", "customers", "user"}, allEntries = true)
    public Output execute(Input input) {
        if (input.userId() == null) {
            throw new InvalidUserException("User ID cannot be null");
        }

        var user = repository.findById(input.userId());

        if (user.isEmpty()) {
            throw new InvalidUserException("Could not found the user with id: %d".formatted(input.userId()));
        }

        repository.deleteById(input.userId());

        return new Output(true, "User deleted successfully");
    }
}
