package com.undercontroll.infrastructure.handler;

import com.undercontroll.application.dto.common.ExceptionHandlerResponse;
import com.undercontroll.domain.exception.InvalidUploadException;
import com.undercontroll.domain.exception.StorageUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class StorageExceptionHandler extends GenericExceptionHandler {

    @ExceptionHandler(InvalidUploadException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleInvalidUpload(InvalidUploadException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), InvalidUploadException.CODE);
    }

    @ExceptionHandler(StorageUnavailableException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleUnavailable(StorageUnavailableException ex, HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "Armazenamento de arquivos indisponível. Tente de novo.",
                request.getRequestURI(), StorageUnavailableException.CODE, ex);
    }
}
