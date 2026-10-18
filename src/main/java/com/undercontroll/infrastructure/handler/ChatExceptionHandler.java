package com.undercontroll.infrastructure.handler;

import com.undercontroll.application.dto.common.ExceptionHandlerResponse;
import com.undercontroll.domain.exception.AnaUnavailableException;
import com.undercontroll.domain.exception.ConversationBusyException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ChatExceptionHandler extends GenericExceptionHandler {

    @ExceptionHandler(ConversationBusyException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleBusy(
            ConversationBusyException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                request.getRequestURI(),
                ConversationBusyException.CODE
        );
    }

    @ExceptionHandler(AnaUnavailableException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleUnavailable(
            AnaUnavailableException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                ex.getMessage(),
                request.getRequestURI(),
                AnaUnavailableException.CODE,
                ex
        );
    }
}
