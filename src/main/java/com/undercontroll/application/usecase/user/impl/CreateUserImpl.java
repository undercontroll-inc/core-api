package com.undercontroll.application.usecase.user.impl;

import com.undercontroll.application.usecase.user.CreateUserPort;
import com.undercontroll.domain.enums.PasswordEventType;
import com.undercontroll.application.usecase.auth.CreatePasswordEventPort;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.enums.UserType;
import com.undercontroll.domain.exception.InvalidUserException;
import com.undercontroll.domain.repository.UserRepositoryPort;
import com.undercontroll.application.port.MetricsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreateUserImpl implements CreateUserPort {

    private final UserRepositoryPort userRepositoryPort;
    private final CreatePasswordEventPort createPasswordEventPort;
    private final PasswordEncoder passwordEncoder;
    private final MetricsPort metricsPort;

    @Override
    public Output execute(Input input) {
        try {
            validateCreateUserRequest(input);

            Optional<User> existingUserByEmail = userRepositoryPort.findByEmail(input.email());
            if (existingUserByEmail.isPresent()) {
                throw new InvalidUserException("Email is already in use");
            }

            Optional<User> existingUserByPhone = userRepositoryPort.findByPhone(input.phone());
            if (existingUserByPhone.isPresent()) {
                throw new InvalidUserException("Phone is already in use");
            }

            Optional<User> existingUserByCpf = userRepositoryPort.findByCpf(input.cpf());
            if (existingUserByCpf.isPresent()) {
                throw new InvalidUserException("CPF is already in use");
            }

            String password = input.userType().equals(UserType.ADMINISTRATOR)
                    ? passwordEncoder.encode(input.password())
                    : passwordEncoder.encode(createPasswordEventPort.execute(
                            new CreatePasswordEventPort.Input(
                                    PasswordEventType.CREATE,
                                    null,
                                    input.phone(),
                                    null
                            )
                    ).value());

            User user = User.builder()
                    .name(input.name())
                    .email(input.email())
                    .lastName(input.lastName())
                    .password(password)
                    .address(input.address())
                    .cpf(input.cpf())
                    .CEP(input.CEP())
                    .phone(input.phone())
                    .avatarUrl(input.avatarUrl())
                    .hasWhatsApp(input.hasWhatsApp())
                    .alreadyRecurrent(input.alreadyRecurrent())
                    .inFirstLogin(input.inFirstLogin())
                    .userType(input.userType())
                    .build();

            userRepositoryPort.save(user);
            metricsPort.incrementAccountCreated();

            return new Output(
                    input.name(),
                    input.email(),
                    input.lastName(),
                    input.address(),
                    input.cpf(),
                    input.CEP(),
                    input.phone(),
                    input.avatarUrl(),
                    input.userType()
            );
        } catch (InvalidUserException e) {
            metricsPort.incrementAccountCreationFailed();
            throw e;
        }
    }

    private void validateCreateUserRequest(Input input) {
        if (input.name() == null || input.name().trim().isEmpty()) {
            throw new InvalidUserException("User name cannot be empty");
        }
        if (input.CEP() == null || input.CEP().isEmpty()) {
            throw new InvalidUserException("CEP cannot be empty");
        }
        if (input.phone() == null || input.phone().isEmpty()) {
            throw new InvalidUserException("Phone number cannot be empty");
        }
        if (input.address() == null || input.address().trim().isEmpty()) {
            throw new InvalidUserException("User address cannot be empty");
        }
        if (input.lastName() == null || input.lastName().trim().isEmpty()) {
            throw new InvalidUserException("User last name cannot be empty");
        }
        if (input.password() == null || input.password().trim().isEmpty()) {
            throw new InvalidUserException("User password cannot be empty");
        }
    }
}
