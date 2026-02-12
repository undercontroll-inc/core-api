package com.undercontroll.infrastructure.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends GenericExceptionHandler {
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<ExceptionHandlerResponse> handleMethodArgumentNotValidException(
//            MethodArgumentNotValidException e,
//            HttpServletRequest request
//    ) {
//        log.error("Validation error: {}", e.getMessage());
//
//        return this.buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), request.getRequestURI());
//    }

}
