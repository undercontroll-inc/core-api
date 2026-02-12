package com.undercontroll.application.service;

import com.undercontroll.domain.entity.enums.UserType;

public interface TokenService {

    String generateToken(String username, UserType userType);

    String extractUsername(String token);
}
