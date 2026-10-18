package com.undercontroll.domain.usecase.user.impl;

import com.undercontroll.domain.usecase.user.UpdateUserPort;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.exception.UserNotFoundException;
import com.undercontroll.domain.gateway.UserGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UpdateUserImpl implements UpdateUserPort {

    private final UserGateway userGateway;
    private final PasswordEncoder passwordEncoder;

    @Override
    @CacheEvict(value = {"users", "customers", "user"}, allEntries = true)
    public void execute(Integer userId, Input request) {
        Optional<User> user = userGateway.findById(userId);

        if (user.isEmpty()) {
            throw new UserNotFoundException("Could not find the user to update with id: %d".formatted(userId));
        }

        User userFound = user.get();

        if (request.name() != null) {
            userFound.setName(request.name());
        }
        if (request.lastName() != null) {
            userFound.setLastName(request.lastName());
        }
        if (request.address() != null) {
            userFound.setAddress(request.address());
        }
        if (request.userType() != null) {
            userFound.setUserType(request.userType());
        }
        if (request.cpf() != null) {
            userFound.setCpf(request.cpf());
        }
        if (request.password() != null && !request.password().isBlank()) {
            userFound.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.hasWhatsApp() != null) {
            userFound.setHasWhatsApp(request.hasWhatsApp());
        }
        if (request.CEP() != null) {
            userFound.setCEP(request.CEP());
        }
        if (request.alreadyRecurrent() != null) {
            userFound.setAlreadyRecurrent(request.alreadyRecurrent());
        }
        if (request.inFirstLogin() != null) {
            userFound.setInFirstLogin(request.inFirstLogin());
        }
        if (request.phone() != null) {
            userFound.setPhone(request.phone());
        }
        if (request.avatarUrl() != null) {
            userFound.setAvatarUrl(request.avatarUrl());
        }

        userGateway.save(userFound);
    }
}
