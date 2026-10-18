package com.undercontroll.domain.usecase.user.impl;

import com.undercontroll.domain.usecase.user.CreateUserPort;
import com.undercontroll.domain.enums.PasswordEventType;
import com.undercontroll.domain.usecase.auth.CreatePasswordEventPort;
import com.undercontroll.domain.gateway.NotificationGateway;
import com.undercontroll.domain.event.UserCreatedEvent;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.enums.UserType;
import com.undercontroll.domain.exception.InvalidUserException;
import com.undercontroll.domain.gateway.UserGateway;
import com.undercontroll.domain.gateway.MetricsGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateUserImpl implements CreateUserPort {

    private final UserGateway userGateway;
    private final CreatePasswordEventPort createPasswordEventPort;
    private final PasswordEncoder passwordEncoder;
    private final MetricsGateway metricsGateway;
    private final NotificationGateway notificationGateway;

    @Override
    public Output execute(Input request) {
        try {
            validateCreateUserRequest(request);

            Optional<User> existingUserByEmail = userGateway.findByEmail(request.email());
            if (existingUserByEmail.isPresent()) {
                throw new InvalidUserException("Email is already in use");
            }

            Optional<User> existingUserByPhone = userGateway.findByPhone(request.phone());
            if (existingUserByPhone.isPresent()) {
                throw new InvalidUserException("Phone is already in use");
            }

            Optional<User> existingUserByCpf = userGateway.findByCpf(request.cpf());
            if (existingUserByCpf.isPresent()) {
                throw new InvalidUserException("CPF is already in use");
            }

            String password = passwordEncoder.encode(
                    request.userType().equals(UserType.ADMINISTRATOR)
                            ? request.password()
                            : createPasswordEventPort.execute(
                                    new CreatePasswordEventPort.Input(
                                            PasswordEventType.CREATE,
                                            null,
                                            request.phone(),
                                            null
                                    )
                            ).getValue()
            );

            User user = User.builder()
                    .name(request.name())
                    .email(request.email())
                    .lastName(request.lastName())
                    .password(password)
                    .address(request.address())
                    .cpf(request.cpf())
                    .CEP(request.CEP())
                    .phone(request.phone())
                    .avatarUrl(request.avatarUrl())
                    .hasWhatsApp(request.hasWhatsApp())
                    .alreadyRecurrent(request.alreadyRecurrent())
                    .inFirstLogin(request.inFirstLogin())
                    .userType(request.userType())
                    .build();

            User createdUser = userGateway.save(user);

            if (UserType.CUSTOMER.equals(createdUser.getUserType())) {
                notificationGateway.handleUserCreated(new UserCreatedEvent(
                        createdUser.getName(),
                        createdUser.getEmail(),
                        createdUser.getCreatedAt()
                ));
            }

            metricsGateway.incrementAccountCreated();
            log.info("User created userId={} type={}", createdUser.getId(), createdUser.getUserType());
            return new Output(
                    createdUser.getName(),
                    createdUser.getEmail(),
                    createdUser.getLastName(),
                    createdUser.getAddress(),
                    createdUser.getCpf(),
                    createdUser.getCEP(),
                    createdUser.getPhone(),
                    createdUser.getAvatarUrl(),
                    createdUser.getUserType()
            );
        } catch (InvalidUserException e) {
            metricsGateway.incrementAccountCreationFailed();
            throw e;
        }
    }

    private void validateCreateUserRequest(Input request) {
        if (request.name() == null || request.name().trim().isEmpty()) {
            throw new InvalidUserException("User name cannot be empty");
        }
        if (request.CEP() == null || request.CEP().isEmpty()) {
            throw new InvalidUserException("CEP cannot be empty");
        }
        if (request.phone() == null || request.phone().isEmpty()) {
            throw new InvalidUserException("Phone number cannot be empty");
        }
        if (request.address() == null || request.address().trim().isEmpty()) {
            throw new InvalidUserException("User address cannot be empty");
        }
        if (request.lastName() == null || request.lastName().trim().isEmpty()) {
            throw new InvalidUserException("User last name cannot be empty");
        }
        if (request.password() == null || request.password().trim().isEmpty()) {
            throw new InvalidUserException("User password cannot be empty");
        }
    }
}
