package com.undercontroll.infrastructure.handler;

import com.undercontroll.application.dto.common.ExceptionHandlerResponse;
import com.undercontroll.domain.exception.AnaUnavailableException;
import com.undercontroll.domain.exception.ConversationBusyException;
import com.undercontroll.domain.exception.ConversationNotFoundException;
import com.undercontroll.domain.exception.InvalidChatHistoryRequestException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

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

    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleConversationNotFound(
            ConversationNotFoundException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getRequestURI(),
                ConversationNotFoundException.CODE
        );
    }

    @ExceptionHandler(InvalidChatHistoryRequestException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleInvalidHistoryRequest(
            InvalidChatHistoryRequestException ex,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI(),
                VALIDATION_ERROR,
                List.of(new ExceptionHandlerResponse.FieldError(ex.field(), ex.getMessage()))
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
