package com.undercontroll.presentation.handler;

import com.undercontroll.domain.exception.*;
import com.undercontroll.presentation.dto.ExceptionHandlerResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OrderExceptionHandler extends GenericExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleDuplicatedWallet(
            OrderNotFoundException ex, HttpServletRequest request
    ) {

        return this.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UnauthorizedOrderOperation.class)
    public ResponseEntity<ExceptionHandlerResponse> handleUnauthorizedOrderOperation(
            UnauthorizedOrderOperation ex, HttpServletRequest request
    ) {

        return this.buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvalidDeleteOrderException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleInvalidDeleteOrderException(
            InvalidDeleteOrderException ex, HttpServletRequest request
    ) {

        return this.buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvalidOrderDateException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleInvalidOrderDateException(
            InvalidOrderDateException ex, HttpServletRequest request
    ) {

        return this.buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvalidUpdateOrderException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleInvalidUpdateOrderException(
            InvalidUpdateOrderException ex, HttpServletRequest request
    ) {

        return this.buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ImmutableOrderException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleImmutableOrderException(
            ImmutableOrderException ex, HttpServletRequest request
    ) {

        return this.buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InsuficientComponentException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleInsuficientComponentException(
            InsuficientComponentException ex, HttpServletRequest request
    ) {

        return this.buildErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI());
    }

}
