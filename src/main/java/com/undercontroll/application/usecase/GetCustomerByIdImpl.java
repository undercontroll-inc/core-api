package com.undercontroll.application.usecase;

import com.undercontroll.domain.model.User;
import com.undercontroll.domain.port.in.GetCustomerByIdPort;
import com.undercontroll.domain.port.out.UserRepositoryPort;
import com.undercontroll.application.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetCustomerByIdImpl implements GetCustomerByIdPort {

    private final UserRepositoryPort userRepositoryPort;

    @Override
    public Output execute(Input input) {
        Optional<User> customer = userRepositoryPort.findCustomerById(input.customerId());
        if (customer.isEmpty()) {
            return new Output(null);
        }
        User u = customer.get();
        return new Output(new UserDto(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getLastName(),
                u.getAddress(),
                u.getCpf(),
                u.getCEP(),
                u.getPhone(),
                u.getAvatarUrl(),
                u.getHasWhatsApp(),
                u.getAlreadyRecurrent(),
                u.getInFirstLogin(),
                u.getUserType()
        ));
    }
}
