package com.undercontroll.application.mapper;

import com.undercontroll.application.dto.auth.ResetPasswordRequest;
import com.undercontroll.application.dto.user.CreateUserRequest;
import com.undercontroll.application.dto.user.CreateUserResponse;
import com.undercontroll.application.dto.user.UpdateUserRequest;
import com.undercontroll.application.dto.user.UserDto;
import com.undercontroll.domain.usecase.auth.ResetPasswordPort;
import com.undercontroll.domain.usecase.user.CreateUserPort;
import com.undercontroll.domain.usecase.user.UpdateUserPort;
import com.undercontroll.domain.usecase.user.UserOutput;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserDtoMapper {

    UserDto toDto(UserOutput output);

    CreateUserPort.Input toInput(CreateUserRequest request);

    UpdateUserPort.Input toInput(UpdateUserRequest request);

    ResetPasswordPort.Input toInput(ResetPasswordRequest request);

    CreateUserResponse toResponse(CreateUserPort.Output output);
}
