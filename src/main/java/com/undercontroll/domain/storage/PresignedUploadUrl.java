package com.undercontroll.domain.storage;

import java.time.Instant;
import java.util.Map;

public record PresignedUploadUrl(String url, String method, Map<String, String> headers, String key, Instant expiresAt) {
}
