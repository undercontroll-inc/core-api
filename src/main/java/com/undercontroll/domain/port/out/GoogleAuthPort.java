package com.undercontroll.domain.port.out;

public interface GoogleAuthPort {
    boolean verify(String idToken, String expectedEmail);
}
