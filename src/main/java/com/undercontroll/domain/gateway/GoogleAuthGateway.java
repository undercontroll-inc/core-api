package com.undercontroll.domain.gateway;

public interface GoogleAuthGateway {
    boolean verify(String idToken, String expectedEmail);
}
