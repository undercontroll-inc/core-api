package com.undercontroll.application.mapper;

import com.undercontroll.application.dto.auth.AuthUserRequest;
import com.undercontroll.application.dto.auth.AuthUserResponse;
import com.undercontroll.application.dto.auth.RefreshTokenResponse;
import com.undercontroll.domain.usecase.auth.AuthUserPort;
import com.undercontroll.domain.usecase.auth.RefreshTokenPort;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = UserDtoMapper.class)
public interface AuthDtoMapper {

    AuthUserPort.Input toInput(AuthUserRequest request);

    AuthUserResponse toResponse(AuthUserPort.Output output);

    RefreshTokenResponse toResponse(RefreshTokenPort.Output output);
}
