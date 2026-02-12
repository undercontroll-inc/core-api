package com.undercontroll.application.usecase;

import com.undercontroll.domain.port.in.AuthUserPort;
import com.undercontroll.domain.entity.User;
import com.undercontroll.domain.exception.InvalidAuthException;
import com.undercontroll.infrastructure.persistence.repository.UserJpaRepository;
import com.undercontroll.application.service.TokenService;
import com.undercontroll.application.service.MetricsService;
import com.undercontroll.infrastructure.web.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthUserImpl implements AuthUserPort {

    private final UserJpaRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final MetricsService metricsService;

    @Override
    public Output execute(Input input) {
        try {
            Optional<User> userFound = repository.findUserByEmail(input.email());

            if (userFound.isEmpty()) {
                metricsService.incrementLoginFailed();
                throw new InvalidAuthException("Email or password is invalid");
            }

            boolean passwordMatch = passwordEncoder.matches(input.password(), userFound.get().getPassword());

            if (!passwordMatch) {
                metricsService.incrementLoginFailed();
                throw new InvalidAuthException("Email or password is invalid");
            }

            User user = userFound.get();
            String token = tokenService.generateToken(user.getEmail(), user.getUserType());

            metricsService.incrementLoginSuccess();

            return new Output(
                    token,
                    mapToDto(user)
            );
        } catch (InvalidAuthException e) {
            throw e;
        }
    }

    private UserDto mapToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getLastName(),
                user.getAddress(),
                user.getCpf(),
                user.getCEP(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getHasWhatsApp(),
                user.getAlreadyRecurrent(),
                user.getInFirstLogin(),
                user.getUserType()
        );
    }
}
