package com.undercontroll.domain.exception;

public class OrderPhotoLimitException extends RuntimeException {

    public static final String CODE = "ORDER_PHOTO_LIMIT";

    public OrderPhotoLimitException(int max) {
        super("Cada pedido aceita no máximo " + max + " fotos");
    }
}
