package com.undercontroll.domain.port.out;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.undercontroll.domain.model.enums.UserType;

public interface TokenPort {

    String generateToken(String username, UserType userType);
    DecodedJWT validateToken(String token);
    String extractUsername(String token);
}
