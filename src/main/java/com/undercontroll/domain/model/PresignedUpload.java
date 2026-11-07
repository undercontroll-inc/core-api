package com.undercontroll.domain.model;

public record PresignedUpload(String presignedUrl, String fileKey, Long expirationTime) {
}
