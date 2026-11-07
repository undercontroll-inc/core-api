package com.undercontroll.domain.gateway;

import com.undercontroll.domain.enums.UserType;
import com.undercontroll.domain.model.AccessTokenClaims;

public interface TokenGateway {

    String generateToken(String userId, UserType userType);

    AccessTokenClaims validateToken(String token);
}
