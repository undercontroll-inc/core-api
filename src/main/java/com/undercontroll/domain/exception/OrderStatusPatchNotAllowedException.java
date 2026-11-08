package com.undercontroll.domain.exception;

public class OrderStatusPatchNotAllowedException extends RuntimeException {

    public OrderStatusPatchNotAllowedException() {
        super("O status não pode mais ser alterado por PATCH. Use POST /v1/api/orders/{id}/transitions");
    }
}
