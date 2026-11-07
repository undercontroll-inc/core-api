package com.undercontroll.domain.exception;

import com.undercontroll.domain.model.insight.LlmFailure;

public class InvoiceExtractionException extends RuntimeException {

    private final LlmFailure failure;

    public InvoiceExtractionException(LlmFailure failure, String message, Throwable cause) {
        super(message, cause);
        this.failure = failure;
    }

    public LlmFailure failure() {
        return failure;
    }
}
