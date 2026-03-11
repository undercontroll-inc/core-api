package com.undercontroll.application.port;

public interface GoogleAuthPort {
    boolean verify(String idToken, String expectedEmail);
}
