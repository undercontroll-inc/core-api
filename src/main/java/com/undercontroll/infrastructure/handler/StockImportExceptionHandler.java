package com.undercontroll.infrastructure.handler;

import com.undercontroll.application.controller.impl.StockImportController;
import com.undercontroll.application.dto.common.ExceptionHandlerResponse;
import com.undercontroll.domain.exception.FieldViolation;
import com.undercontroll.domain.exception.InvalidStockImportException;
import com.undercontroll.domain.exception.StockImportComponentNotFoundException;
import com.undercontroll.domain.exception.StockImportInvalidStateException;
import com.undercontroll.domain.exception.StockImportNotFoundException;
import com.undercontroll.domain.exception.StockImportUnavailableException;
import com.undercontroll.domain.exception.StockImportValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RestControllerAdvice(assignableTypes = StockImportController.class)
public class StockImportExceptionHandler extends GenericExceptionHandler {

    @ExceptionHandler(InvalidStockImportException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleInvalid(InvalidStockImportException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(),
                InvalidStockImportException.CODE);
    }

    @ExceptionHandler(StockImportValidationException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleValidation(StockImportValidationException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), VALIDATION_ERROR,
                fieldErrors(ex.violations()));
    }

    @ExceptionHandler(StockImportNotFoundException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleNotFound(StockImportNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(),
                StockImportNotFoundException.CODE);
    }

    @ExceptionHandler(StockImportInvalidStateException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleState(StockImportInvalidStateException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(),
                StockImportInvalidStateException.CODE);
    }

    @ExceptionHandler(StockImportComponentNotFoundException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleComponent(StockImportComponentNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI(),
                StockImportComponentNotFoundException.CODE, fieldErrors(ex.violations()));
    }

    @ExceptionHandler(StockImportUnavailableException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleUnavailable(StockImportUnavailableException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request.getRequestURI(),
                StockImportUnavailableException.CODE);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ExceptionHandlerResponse> handleUnreadable(Exception ex, HttpServletRequest request) {
        String message = ex instanceof MethodArgumentTypeMismatchException mismatch
                ? "Invalid value for " + mismatch.getName()
                : "Malformed request body";
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI(), VALIDATION_ERROR);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage(), request.getRequestURI(),
                VALIDATION_ERROR);
    }

    private static List<ExceptionHandlerResponse.FieldError> fieldErrors(List<FieldViolation> violations) {
        return violations.stream()
                .map(violation -> new ExceptionHandlerResponse.FieldError(violation.field(), violation.message()))
                .toList();
    }
}
