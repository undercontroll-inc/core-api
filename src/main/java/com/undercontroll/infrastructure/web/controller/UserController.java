package com.undercontroll.infrastructure.web.controller;

import com.undercontroll.domain.port.in.*;
import com.undercontroll.infrastructure.web.api.UserApi;
import com.undercontroll.application.dto.*;
import com.undercontroll.infrastructure.web.dto.*;
import com.undercontroll.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/v1/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final CreateUserPort createUserPort;
    private final AuthUserPort authUserPort;
    private final UpdateUserPort updateUserPort;
    private final GetUsersPort getUsersPort;
    private final GetCustomersPort getCustomersPort;
    private final GetCustomerByIdPort getCustomerByIdPort;
    private final GetCustomersWithEmailPort getCustomersWithEmailPort;
    private final GetUserPort getUserPort;
    private final DeleteUserPort deleteUserPort;
    private final ResetPasswordPort resetPasswordPort;

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CreateUserResponse> createUser(@RequestBody CreateUserRequest request) {
        var output = createUserPort.execute(new CreateUserPort.Input(
                request.name(),
                request.email(),
                request.phone(),
                request.lastName(),
                request.password(),
                request.address(),
                request.cpf(),
                request.avatarUrl(),
                request.userType(),
                request.hasWhatsApp(),
                request.alreadyRecurrent(),
                request.inFirstLogin(),
                request.CEP()
        ));
        return ResponseEntity.status(201).body(new CreateUserResponse(
                output.name(),
                output.email(),
                output.lastName(),
                output.address(),
                output.cpf(),
                output.CEP(),
                output.phone(),
                output.avatarUrl(),
                output.userType()
        ));
    }

    @Override
    @PostMapping(value = "/auth", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthUserResponse> auth(@RequestBody AuthUserRequest request) {
        var output = authUserPort.execute(new AuthUserPort.Input(request.email(), request.password()));
        return ResponseEntity.ok(new AuthUserResponse(output.token(), output.user()));
    }

    @Override
    @PostMapping(value = "/auth/google", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthUserResponse> authGoogle(@RequestBody AuthGoogleRequest request) {
        var output = authUserPort.execute(new AuthUserPort.Input(request.email(), null));
        return ResponseEntity.ok(new AuthUserResponse(output.token(), output.user()));
    }

    @Override
    @PutMapping(value = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateUser(@RequestBody UpdateUserRequest request, @PathVariable Integer userId) {
        updateUserPort.execute(new UpdateUserPort.Input(
                userId,
                request.name(),
                request.lastName(),
                request.address(),
                request.cpf(),
                request.password(),
                request.hasWhatsApp(),
                request.CEP(),
                request.alreadyRecurrent(),
                request.inFirstLogin(),
                request.phone(),
                request.avatarUrl(),
                request.userType()
        ));
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping
    public ResponseEntity<List<UserDto>> getUsers() {
        var output = getUsersPort.execute(new GetUsersPort.Input());
        return output.users().isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(output.users());
    }

    @Override
    @GetMapping("/customers")
    public ResponseEntity<List<UserDto>> getCostumers() {
        var output = getCustomersPort.execute(new GetCustomersPort.Input());
        return output.customers().isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(output.customers());
    }

    @Override
    @GetMapping("/customers/{customerId}")
    public ResponseEntity<UserDto> getCostumerById(@PathVariable Integer customerId) {
        var output = getCustomerByIdPort.execute(new GetCustomerByIdPort.Input(customerId));
        return output.customer() != null ? ResponseEntity.ok(output.customer()) : ResponseEntity.notFound().build();
    }

    @Override
    @GetMapping("/customers/emails")
    public ResponseEntity<List<UserDto>> getCustomersThatHaveEmail() {
        var output = getCustomersWithEmailPort.execute(new GetCustomersWithEmailPort.Input());
        return output.customers().isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(output.customers());
    }

    @Override
    @GetMapping("/{userId}")
    public ResponseEntity<User> getUserById(@PathVariable Integer userId) {
        var output = getUserPort.execute(new GetUserPort.Input(userId));
        return output.user() != null ? ResponseEntity.ok(output.user()) : ResponseEntity.notFound().build();
    }

    @Override
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer userId) {
        deleteUserPort.execute(new DeleteUserPort.Input(userId));
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
        resetPasswordPort.execute(new ResetPasswordPort.Input(userId, request.newPassword(), token));
        return ResponseEntity.ok().build();
    }

}
