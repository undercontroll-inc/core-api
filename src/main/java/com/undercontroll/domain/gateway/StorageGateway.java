package com.undercontroll.domain.gateway;

import com.undercontroll.application.dto.announcement.GenerateUploadUrlResponse;
import java.util.Optional;

public interface StorageGateway {

    void putObject(String bucket, String key, byte[] data, Optional<String> contentType);

    GenerateUploadUrlResponse generatePresignedUploadUrl(String bucket, String key, Integer expirationMinutes);
    String generateReadPresignedUrl(String bucket, String key, long expirationMinutes);

}
