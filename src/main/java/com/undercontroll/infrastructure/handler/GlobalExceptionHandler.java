package com.undercontroll.infrastructure.handler;

import com.undercontroll.application.dto.common.ExceptionHandlerResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler extends GenericExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        List<ExceptionHandlerResponse.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new ExceptionHandlerResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();

        return this.buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                request.getRequestURI(),
                VALIDATION_ERROR,
                fieldErrors
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException e,
            HttpServletRequest request
    ) {
        return this.buildErrorResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content-Type " + e.getContentType() + " is not supported here; supported: " + e.getSupportedMediaTypes(),
                request.getRequestURI(),
                VALIDATION_ERROR
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionHandlerResponse> handleUnexpectedException(
            Exception e,
            HttpServletRequest request
    ) {
        return this.buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred while processing the request",
                request.getRequestURI(),
                INTERNAL_SERVER_ERROR,
                e
        );
    }

}
