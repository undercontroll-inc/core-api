package com.undercontroll.infrastructure.web.controller;

import com.undercontroll.infrastructure.web.api.UserApi;
import com.undercontroll.infrastructure.web.dto.*;
import com.undercontroll.domain.entity.User;
import com.undercontroll.application.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/v1/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService service;

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateUserResponse> createUser(@RequestBody CreateUserRequest request) {
        CreateUserResponse user = service.createUser(request);
        return ResponseEntity.status(201).body(user);
    }

    @Override
    @PostMapping(value = "/auth", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthUserResponse> auth(@RequestBody AuthUserRequest request) {
        AuthUserResponse response = service.authUser(request);
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping(value = "/auth/google", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthUserResponse> authGoogle(@RequestBody AuthGoogleRequest request) {
        AuthUserResponse response = service.authUserByGoogle(request);
        return ResponseEntity.ok(response);
    }

    @Override
    @PutMapping(value = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateUser(@RequestBody UpdateUserRequest request, @PathVariable Integer userId) {
        service.updateUser(request, userId);
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping
    public ResponseEntity<List<UserDto>> getUsers() {
        List<UserDto> users = service.getUsers();
        return ResponseEntity.ok(users);
    }

    @Override
    @GetMapping("/customers")
    public ResponseEntity<List<UserDto>> getCostumers() {
        var users = service.getCustomers();
        return users.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(users);
    }

    @Override
    @GetMapping("/customers/{customerId}")
    public ResponseEntity<UserDto> getCostumerById(@PathVariable Integer customerId) {
        var user = service.getCustomersById(customerId);
        return ResponseEntity.ok(user);
    }

    @Override
    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable Integer userId) {
        User user = service.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @Override
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer userId) {
        service.deleteUser(userId);
        return ResponseEntity.ok().build();
    }

    @Override
    @PatchMapping("/reset-password/{userId}")
    public ResponseEntity<Void> resetPassword(
            @RequestBody ResetPasswordRequest request,
            @PathVariable Integer userId,
            @RequestHeader("Authorization") String authHeader
    ) {
        String token = authHeader.substring(7);

        service.resetPassword(request, userId, token);

        return ResponseEntity.ok().build();
    }

}

