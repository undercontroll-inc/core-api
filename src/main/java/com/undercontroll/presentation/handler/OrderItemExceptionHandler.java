package com.undercontroll.presentation.handler;

import com.undercontroll.domain.exception.InvalidOrderItemException;
import com.undercontroll.domain.exception.OrderItemNotFoundException;
import com.undercontroll.presentation.dto.ExceptionHandlerResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class OrderItemExceptionHandler extends GenericExceptionHandler {

    @ExceptionHandler(OrderItemNotFoundException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleOrderItemNotFound(
            OrderItemNotFoundException ex, HttpServletRequest request
    ) {
        return this.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvalidOrderItemException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleInvalidOrderItem(
            InvalidOrderItemException ex, HttpServletRequest request
    ) {
        return this.buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

}