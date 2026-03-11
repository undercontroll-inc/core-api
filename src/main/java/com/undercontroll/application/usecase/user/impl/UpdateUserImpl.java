package com.undercontroll.application.usecase.user.impl;

import com.undercontroll.application.usecase.user.UpdateUserPort;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.exception.InvalidUserException;
import com.undercontroll.domain.repository.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UpdateUserImpl implements UpdateUserPort {

    private final UserRepositoryPort userRepositoryPort;

    @Override
    @CacheEvict(value = {"users", "customers", "user"}, allEntries = true)
    public Output execute(Input input) {
        Optional<User> user = userRepositoryPort.findById(input.userId());

        if (user.isEmpty()) {
            throw new InvalidUserException("Could not found the user for update with id: %d".formatted(input.userId()));
        }

        User userFound = user.get();

        if (input.name() != null) {
            userFound.setName(input.name());
        }
        if (input.lastName() != null) {
            userFound.setLastName(input.lastName());
        }
        if (input.address() != null) {
            userFound.setAddress(input.address());
        }
        if (input.userType() != null) {
            userFound.setUserType(input.userType());
        }
        if (input.cpf() != null) {
            userFound.setCpf(input.cpf());
        }
        if (input.password() != null) {
            userFound.setPassword(input.password());
        }
        if (input.hasWhatsApp() != null) {
            userFound.setHasWhatsApp(input.hasWhatsApp());
        }
        if (input.CEP() != null) {
            userFound.setCEP(input.CEP());
        }
        if (input.alreadyRecurrent() != null) {
            userFound.setAlreadyRecurrent(input.alreadyRecurrent());
        }
        if (input.inFirstLogin() != null) {
            userFound.setInFirstLogin(input.inFirstLogin());
        }
        if (input.phone() != null) {
            userFound.setPhone(input.phone());
        }
        if (input.avatarUrl() != null) {
            userFound.setAvatarUrl(input.avatarUrl());
        }

        userRepositoryPort.save(userFound);

        return new Output(true, "User updated successfully");
    }
}
