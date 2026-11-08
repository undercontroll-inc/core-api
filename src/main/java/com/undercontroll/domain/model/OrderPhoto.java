package com.undercontroll.domain.model;

import com.undercontroll.domain.enums.OrderPhotoKind;

import java.time.Instant;

public record OrderPhoto(
        Long id,
        Integer orderId,
        OrderPhotoKind kind,
        String storageKey,
        String contentType,
        long size,
        Instant createdAt
) {
}
