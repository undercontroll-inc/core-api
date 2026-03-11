package com.undercontroll.application.usecase.user.impl;

import com.undercontroll.application.usecase.user.GetCustomersPort;
import com.undercontroll.domain.model.User;
import com.undercontroll.domain.repository.UserRepositoryPort;
import com.undercontroll.application.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetCustomersImpl implements GetCustomersPort {

    private final UserRepositoryPort userRepositoryPort;

    @Override
    public Output execute(Input input) {
        List<UserDto> customers = userRepositoryPort.findAllCustomers()
                .stream()
                .map(this::mapToDto)
                .toList();
        return new Output(customers);
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
