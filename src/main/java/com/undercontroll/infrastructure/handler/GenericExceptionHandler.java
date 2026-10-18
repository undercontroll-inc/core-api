package com.undercontroll.infrastructure.handler;

import com.undercontroll.application.dto.common.ExceptionHandlerResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public abstract class GenericExceptionHandler {

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";

    protected ResponseEntity<ExceptionHandlerResponse> buildErrorResponse(HttpStatus status, String message, String path) {
        return buildErrorResponse(status, message, path, null, null, null);
    }

    protected ResponseEntity<ExceptionHandlerResponse> buildErrorResponse(
            HttpStatus status,
            String message,
            String path,
            String code
    ) {
        return buildErrorResponse(status, message, path, code, null, null);
    }

    protected ResponseEntity<ExceptionHandlerResponse> buildErrorResponse(
            HttpStatus status,
            String message,
            String path,
            String code,
            List<ExceptionHandlerResponse.FieldError> errors
    ) {
        return buildErrorResponse(status, message, path, code, errors, null);
    }

    protected ResponseEntity<ExceptionHandlerResponse> buildErrorResponse(
            HttpStatus status,
            String message,
            String path,
            String code,
            Throwable cause
    ) {
        return buildErrorResponse(status, message, path, code, null, cause);
    }

    protected ResponseEntity<ExceptionHandlerResponse> buildErrorResponse(
            HttpStatus status,
            String message,
            String path,
            String code,
            List<ExceptionHandlerResponse.FieldError> errors,
            Throwable cause
    ) {
        logHttpError(status, message, path, code, errors, cause);
        ExceptionHandlerResponse error = new ExceptionHandlerResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                LocalDateTime.now(ZoneOffset.UTC),
                code,
                errors);

        return new ResponseEntity<>(error, status);
    }

    private void logHttpError(
            HttpStatus status,
            String message,
            String path,
            String code,
            List<ExceptionHandlerResponse.FieldError> errors,
            Throwable cause
    ) {
        int fieldErrors = errors == null ? 0 : errors.size();
        if (status.is5xxServerError()) {
            if (cause != null) {
                log.error("status={} code={} path={} message={}", status.value(), code, path, message, cause);
            } else {
                log.error("status={} code={} path={} message={}", status.value(), code, path, message);
            }
            return;
        }
        log.warn(
                "status={} code={} path={} message={} fieldErrors={}",
                status.value(),
                code,
                path,
                message,
                fieldErrors
        );
    }

}
