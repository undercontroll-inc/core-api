package com.undercontroll.infrastructure.handler;

import com.undercontroll.application.dto.common.ExceptionHandlerResponse;
import com.undercontroll.domain.exception.InsightPayloadUnreadableException;
import com.undercontroll.domain.exception.InsightsDisabledException;
import com.undercontroll.domain.exception.MarketDataUnavailableException;
import com.undercontroll.domain.exception.MarketSnapshotAwaitingException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MarketExceptionHandler extends GenericExceptionHandler {

    @ExceptionHandler(MarketDataUnavailableException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleUnavailable(
            MarketDataUnavailableException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                ex.getMessage(),
                request.getRequestURI(),
                MarketDataUnavailableException.CODE,
                ex
        );
    }

    @ExceptionHandler(MarketSnapshotAwaitingException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleAwaiting(
            MarketSnapshotAwaitingException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(),
                MarketSnapshotAwaitingException.CODE);
    }

    @ExceptionHandler(InsightsDisabledException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleDisabled(
            InsightsDisabledException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request.getRequestURI(),
                InsightsDisabledException.CODE);
    }

    @ExceptionHandler(InsightPayloadUnreadableException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleUnreadable(
            InsightPayloadUnreadableException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI(),
                InsightPayloadUnreadableException.CODE, ex);
    }
}
