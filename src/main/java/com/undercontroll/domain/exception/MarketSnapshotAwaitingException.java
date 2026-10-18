package com.undercontroll.domain.exception;

public class MarketSnapshotAwaitingException extends RuntimeException {

    public static final String CODE = "AWAITING_DATA";

    public MarketSnapshotAwaitingException() {
        super("No market snapshot has been published by the ETL yet");
    }
}
