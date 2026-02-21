package com.undercontroll.application.usecase;

import com.undercontroll.domain.exception.InvalidAuthException;
import com.undercontroll.domain.port.in.AuthUserPort;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.port.out.UserRepositoryPort;
import com.undercontroll.domain.port.out.TokenPort;
import com.undercontroll.domain.port.out.MetricsPort;
import com.undercontroll.infrastructure.web.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthUserImpl implements AuthUserPort {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final TokenPort tokenPort;
    private final MetricsPort metricsPort;

    @Override
    public Output execute(Input input) {
        try {
            Optional<User> userFound = userRepositoryPort.findByEmail(input.email());

            if (userFound.isEmpty()) {
                metricsPort.incrementLoginFailed();
                throw new InvalidAuthException("Email or password is invalid");
            }

            boolean passwordMatch = passwordEncoder.matches(input.password(), userFound.get().getPassword());

            if (!passwordMatch) {
                metricsPort.incrementLoginFailed();
                throw new InvalidAuthException("Email or password is invalid");
            }

            User user = userFound.get();
            String token = tokenPort.generateToken(user.getEmail(), user.getUserType());

            metricsPort.incrementLoginSuccess();

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
