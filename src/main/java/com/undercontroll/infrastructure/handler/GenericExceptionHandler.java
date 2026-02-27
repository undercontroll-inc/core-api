package com.undercontroll.infrastructure.handler;

import com.undercontroll.infrastructure.web.dto.ExceptionHandlerResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public abstract class GenericExceptionHandler {

    protected ResponseEntity<ExceptionHandlerResponse> buildErrorResponse(HttpStatus status, String message, String path) {
        ExceptionHandlerResponse error = new ExceptionHandlerResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                LocalDateTime.now());

        return new ResponseEntity<>(error, status);
    }

}