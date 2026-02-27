package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.DeleteUserPort;
import com.undercontroll.domain.exception.InvalidUserException;
import com.undercontroll.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteUserImpl implements DeleteUserPort {

    private final UserRepositoryPort userRepositoryPort;

    @Override
    @CacheEvict(value = {"users", "customers", "user"}, allEntries = true)
    public Output execute(Input input) {
        if (input.userId() == null) {
            throw new InvalidUserException("User ID cannot be null");
        }

        var user = userRepositoryPort.findById(input.userId());

        if (user.isEmpty()) {
            throw new InvalidUserException("Could not found the user with id: %d".formatted(input.userId()));
        }

        userRepositoryPort.deleteById(input.userId());

        return new Output(true, "User deleted successfully");
    }
}
