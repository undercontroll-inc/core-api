package com.undercontroll.domain.exception;

public class OrderPhotoNotFoundException extends RuntimeException {

    public static final String CODE = "ORDER_PHOTO_NOT_FOUND";

    public OrderPhotoNotFoundException(Long photoId) {
        super("Photo " + photoId + " not found");
    }
}
