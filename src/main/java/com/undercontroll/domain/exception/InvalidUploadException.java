package com.undercontroll.domain.exception;

public class InvalidUploadException extends RuntimeException {

    public static final String CODE = "UPLOAD_INVALID";

    public InvalidUploadException(String message) {
        super(message);
    }
}
