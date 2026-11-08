package com.undercontroll.infrastructure.handler;

import com.undercontroll.application.dto.common.ExceptionHandlerResponse;
import com.undercontroll.domain.exception.InvalidTranscriptionException;
import com.undercontroll.domain.exception.TranscriptionUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class TranscriptionExceptionHandler extends GenericExceptionHandler {

    @ExceptionHandler(TranscriptionUnavailableException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleUnavailable(
            TranscriptionUnavailableException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                ex.getMessage(),
                request.getRequestURI(),
                TranscriptionUnavailableException.CODE,
                ex
        );
    }

    @ExceptionHandler(InvalidTranscriptionException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleInvalid(
            InvalidTranscriptionException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI(),
                InvalidTranscriptionException.CODE
        );
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleMissingPart(
            MissingServletRequestPartException ex,
            HttpServletRequest request
    ) {
        String path = request.getRequestURI();
        if (path != null && path.contains("/v1/api/transcriptions")) {
            return buildErrorResponse(
                    HttpStatus.BAD_REQUEST,
                    "Audio file is required",
                    path,
                    InvalidTranscriptionException.CODE
            );
        }
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                path,
                VALIDATION_ERROR
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleTooLarge(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ) {
        String path = request.getRequestURI();
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Audio file exceeds 8MB",
                path,
                InvalidTranscriptionException.CODE
        );
    }

}
