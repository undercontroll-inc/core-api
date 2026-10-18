package com.undercontroll.application.controller;

import com.undercontroll.application.controller.doc.ApiResponseDocumentation.*;
import com.undercontroll.application.dto.auth.ResetPasswordRequest;
import com.undercontroll.application.dto.user.CreateUserRequest;
import com.undercontroll.application.dto.user.CreateUserResponse;
import com.undercontroll.application.dto.user.UpdateUserRequest;
import com.undercontroll.application.dto.user.UserDto;
import com.undercontroll.domain.enums.UserType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Users", description = "User management APIs")
@RequestMapping(value = "/v1/api/users")
public interface UserApi {

    @Operation(summary = "Create a new user")
    @PostApiResponses
    @PostMapping
    ResponseEntity<CreateUserResponse> createUser(@RequestBody CreateUserRequest request);

    @Operation(summary = "Update a user")
    @PatchApiResponses
    @SecurityRequirement(name = "Bearer Authentication")
    @PatchMapping("/{userId}")
    ResponseEntity<Void> updateUser(
            @RequestBody UpdateUserRequest request,
            @PathVariable @Parameter(example = "1") Integer userId
    );

    @Operation(summary = "List users, optionally filtered by type")
    @GetApiResponses
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping
    ResponseEntity<List<UserDto>> getUsers(
            @RequestParam(required = false) @Parameter(description = "Filter by user type") UserType type,
            @RequestParam(required = false) @Parameter(description = "When type=CUSTOMER, filter to customers that have an email registered") Boolean hasEmail
    );

    @Operation(summary = "Get a user by id")
    @GetApiResponses
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/{userId}")
    ResponseEntity<UserDto> getUserById(@PathVariable @Parameter(example = "1") Integer userId);

    @Operation(summary = "Delete a user")
    @DeleteApiResponses
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/{userId}")
    ResponseEntity<Void> deleteUser(@PathVariable @Parameter(example = "1") Integer userId);

    @Operation(summary = "Change a user's password")
    @PatchApiResponses
    @SecurityRequirement(name = "Bearer Authentication")
    @PatchMapping("/{userId}/password")
    ResponseEntity<Void> changePassword(
            @RequestBody ResetPasswordRequest request,
            @PathVariable @Parameter(example = "1") Integer userId,
            @RequestHeader("Authorization") String authHeader
    );
}
