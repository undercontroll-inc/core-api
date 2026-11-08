package com.undercontroll.domain.exception;

public class TrackingNotFoundException extends RuntimeException {

    public static final String CODE = "TRACKING_NOT_FOUND";

    public TrackingNotFoundException() {
        super("Link não encontrado. Fale com a loja.");
    }
}
