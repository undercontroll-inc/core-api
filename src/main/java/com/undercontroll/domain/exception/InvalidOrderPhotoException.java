package com.undercontroll.domain.exception;

public class InvalidOrderPhotoException extends RuntimeException {

    public static final String CODE = "ORDER_PHOTO_INVALID";

    public InvalidOrderPhotoException(String message) {
        super(message);
    }
}
