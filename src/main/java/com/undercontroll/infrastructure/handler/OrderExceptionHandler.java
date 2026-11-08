package com.undercontroll.infrastructure.handler;

import com.undercontroll.domain.exception.*;
import com.undercontroll.application.dto.common.ExceptionHandlerResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class OrderExceptionHandler extends GenericExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleOrderNotFound(
            OrderNotFoundException ex, HttpServletRequest request
    ) {
        return this.buildErrorResponse(
                HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), OrderNotFoundException.CODE
        );
    }

    @ExceptionHandler(UnauthorizedOrderOperation.class)
    public ResponseEntity<ExceptionHandlerResponse> handleUnauthorizedOrderOperation(
            UnauthorizedOrderOperation ex, HttpServletRequest request
    ) {
        return this.buildErrorResponse(
                HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI(), UnauthorizedOrderOperation.CODE
        );
    }

    @ExceptionHandler(InvalidDeleteOrderException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleInvalidDeleteOrderException(
            InvalidDeleteOrderException ex, HttpServletRequest request
    ) {
        return this.buildErrorResponse(
                HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), InvalidDeleteOrderException.CODE
        );
    }

    @ExceptionHandler(InvalidOrderDateException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleInvalidOrderDateException(
            InvalidOrderDateException ex, HttpServletRequest request
    ) {
        return this.buildErrorResponse(
                HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), InvalidOrderDateException.CODE
        );
    }

    @ExceptionHandler(InvalidUpdateOrderException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleInvalidUpdateOrderException(
            InvalidUpdateOrderException ex, HttpServletRequest request
    ) {
        return this.buildErrorResponse(
                HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), InvalidUpdateOrderException.CODE
        );
    }

    @ExceptionHandler(ImmutableOrderException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleImmutableOrderException(
            ImmutableOrderException ex, HttpServletRequest request
    ) {
        return this.buildErrorResponse(
                HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), ImmutableOrderException.CODE
        );
    }

    @ExceptionHandler(InsuficientComponentException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleInsuficientComponentException(
            InsuficientComponentException ex, HttpServletRequest request
    ) {
        return this.buildErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI(), InsuficientComponentException.CODE
        );
    }

    @ExceptionHandler(PdfGenerationException.class)
    public ResponseEntity<ExceptionHandlerResponse> handlePdfGenerationException(
            PdfGenerationException ex, HttpServletRequest request
    ) {
        return this.buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI(), PdfGenerationException.CODE, ex
        );
    }

    @ExceptionHandler(TemplateLoadException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleTemplateLoadException(
            TemplateLoadException ex, HttpServletRequest request
    ) {
        return this.buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI(), TemplateLoadException.CODE, ex
        );
    }

    @ExceptionHandler(TempFileException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleTempFileException(
            TempFileException ex, HttpServletRequest request
    ) {
        return this.buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI(), TempFileException.CODE, ex
        );
    }


    @ExceptionHandler(InvalidOrderTransitionException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleInvalidTransition(
            InvalidOrderTransitionException ex, HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), InvalidOrderTransitionException.CODE);
    }

    @ExceptionHandler(OrderConcurrentUpdateException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleConcurrentUpdate(
            OrderConcurrentUpdateException ex, HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, "O pedido foi alterado por outra pessoa. Atualize e tente de novo.",
                request.getRequestURI(), OrderConcurrentUpdateException.CODE);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleOptimisticLock(
            ObjectOptimisticLockingFailureException ex, HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, "O pedido foi alterado por outra pessoa. Atualize e tente de novo.",
                request.getRequestURI(), OrderConcurrentUpdateException.CODE);
    }

    @ExceptionHandler(OrderBudgetEmptyException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleBudgetEmpty(
            OrderBudgetEmptyException ex, HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), OrderBudgetEmptyException.CODE);
    }

    @ExceptionHandler(OrderStatusPatchNotAllowedException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleStatusPatch(
            OrderStatusPatchNotAllowedException ex, HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), VALIDATION_ERROR,
                List.of(new ExceptionHandlerResponse.FieldError("status", ex.getMessage())));
    }

    @ExceptionHandler(TrackingNotFoundException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleTrackingNotFound(
            TrackingNotFoundException ex, HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), TrackingNotFoundException.CODE);
    }

    @ExceptionHandler(InvalidOrderPhotoException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleInvalidPhoto(
            InvalidOrderPhotoException ex, HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), InvalidOrderPhotoException.CODE);
    }

    @ExceptionHandler(OrderPhotoLimitException.class)
    public ResponseEntity<ExceptionHandlerResponse> handlePhotoLimit(
            OrderPhotoLimitException ex, HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), OrderPhotoLimitException.CODE);
    }

    @ExceptionHandler(OrderPhotoNotFoundException.class)
    public ResponseEntity<ExceptionHandlerResponse> handlePhotoNotFound(
            OrderPhotoNotFoundException ex, HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), OrderPhotoNotFoundException.CODE);
    }

    @ExceptionHandler(InvalidLabelReadingException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleInvalidLabel(
            InvalidLabelReadingException ex, HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), InvalidLabelReadingException.CODE);
    }

    @ExceptionHandler(LabelReadingUnavailableException.class)
    public ResponseEntity<ExceptionHandlerResponse> handleLabelUnavailable(
            LabelReadingUnavailableException ex, HttpServletRequest request
    ) {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request.getRequestURI(),
                LabelReadingUnavailableException.CODE, ex.getCause());
    }
}
