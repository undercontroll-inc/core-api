package com.undercontroll.infrastructure.handler;

import com.undercontroll.infrastructure.web.controller.UserController;
import com.undercontroll.domain.exception.*;
import com.undercontroll.infrastructure.web.dto.ExceptionHandlerResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackageClasses = { UserController.class })
public class UserExceptionHandler  extends GenericExceptionHandler {

    @ExceptionHandler(InvalidUserException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleInvalidUser(
            InvalidUserException ex, HttpServletRequest request
    ) {
        return this.buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleUserNotFound(
            UserNotFoundException ex, HttpServletRequest request
    ) {
        return this.buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvalidAuthException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleInvalidAuth(
            InvalidAuthException ex, HttpServletRequest request
    ) {
        log.error("Erro de autenticação: {}", ex.getMessage());

        return this.buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(GoogleAccountNotFoundException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleGoogleAccountNotFoundException(
            GoogleAccountNotFoundException ex, HttpServletRequest request
    ) {
        log.error("Google account not found: {}", ex.getMessage());

        return this.buildErrorResponse(HttpStatus.NOT_FOUND, "Google account not found", request.getRequestURI());
    }

    @ExceptionHandler(InvalidPasswordResetException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleInvalidPasswordResetException(
            InvalidPasswordResetException ex, HttpServletRequest request
    ) {

        return this.buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage() , request.getRequestURI());
    }

}
