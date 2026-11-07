package com.undercontroll.domain.exception;

import com.undercontroll.domain.model.insight.LlmFailure;

public class InsightLlmException extends RuntimeException {

    private final transient LlmFailure failure;

    public InsightLlmException(LlmFailure failure, String message, Throwable cause) {
        super(message, cause);
        this.failure = failure;
    }

    public LlmFailure failure() {
        return failure;
    }
}
