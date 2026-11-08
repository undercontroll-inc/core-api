package com.undercontroll.domain.usecase.order;

import com.undercontroll.domain.enums.OrderPhotoKind;

import java.time.Instant;

public record OrderPhotoOutput(Long id, OrderPhotoKind kind, String url, Instant createdAt) {
}
