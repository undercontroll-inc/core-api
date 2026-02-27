package com.undercontroll.application.usecase;

import com.undercontroll.domain.model.User;
import com.undercontroll.domain.port.in.GetUsersPort;
import com.undercontroll.domain.port.out.UserRepositoryPort;
import com.undercontroll.application.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetUsersImpl implements GetUsersPort {

    private final UserRepositoryPort userRepositoryPort;

    @Override
    public Output execute(Input input) {
        List<UserDto> users = userRepositoryPort.findAll()
                .stream()
                .map(this::mapToDto)
                .toList();
        return new Output(users);
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
