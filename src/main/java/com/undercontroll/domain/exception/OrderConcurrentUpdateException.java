package com.undercontroll.domain.exception;

public class OrderConcurrentUpdateException extends RuntimeException {

    public static final String CODE = "ORDER_CONCURRENT_UPDATE";

    public OrderConcurrentUpdateException(Integer orderId, Throwable cause) {
        super("Order " + orderId + " was changed by someone else; reload and try again", cause);
    }
}
