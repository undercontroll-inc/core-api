package com.undercontroll.application.controller.impl;

import com.undercontroll.application.controller.UserApi;
import com.undercontroll.application.dto.auth.ResetPasswordRequest;
import com.undercontroll.application.dto.user.CreateUserRequest;
import com.undercontroll.application.dto.user.CreateUserResponse;
import com.undercontroll.application.dto.user.UpdateUserRequest;
import com.undercontroll.application.dto.user.UserDto;
import com.undercontroll.application.mapper.UserDtoMapper;
import com.undercontroll.domain.enums.UserType;
import com.undercontroll.domain.usecase.auth.ResetPasswordPort;
import com.undercontroll.domain.usecase.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final CreateUserPort createUserPort;
    private final UpdateUserPort updateUserPort;
    private final GetUsersPort getUsersPort;
    private final GetUserPort getUserPort;
    private final DeleteUserPort deleteUserPort;
    private final ResetPasswordPort resetPasswordPort;
    private final UserDtoMapper mapper;

    @Override
    public ResponseEntity<CreateUserResponse> createUser(CreateUserRequest request) {
        return ResponseEntity.status(201).body(mapper.toResponse(createUserPort.execute(mapper.toInput(request))));
    }

    @Override
    public ResponseEntity<Void> updateUser(UpdateUserRequest request, Integer userId) {
        updateUserPort.execute(userId, mapper.toInput(request));
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<List<UserDto>> getUsers(UserType type, Boolean hasEmail) {
        return ResponseEntity.ok(getUsersPort.execute(type, hasEmail).stream().map(mapper::toDto).toList());
    }

    @Override
    public ResponseEntity<UserDto> getUserById(Integer userId) {
        return getUserPort.execute(userId)
                .map(mapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Void> deleteUser(Integer userId) {
        deleteUserPort.execute(userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> changePassword(ResetPasswordRequest request, Integer userId, String authHeader) {
        String token = authHeader.substring(7);
        resetPasswordPort.execute(userId, mapper.toInput(request), token);
        return ResponseEntity.ok().build();
    }
}
