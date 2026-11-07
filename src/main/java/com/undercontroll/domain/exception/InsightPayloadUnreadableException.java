package com.undercontroll.domain.exception;

public class InsightPayloadUnreadableException extends RuntimeException {

    public static final String CODE = "INSIGHT_PAYLOAD_UNREADABLE";

    public InsightPayloadUnreadableException(Throwable cause) {
        super("Published insight payload could not be read", cause);
    }
}
