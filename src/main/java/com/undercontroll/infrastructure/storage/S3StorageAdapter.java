package com.undercontroll.infrastructure.storage;

import com.undercontroll.application.dto.announcement.GenerateUploadUrlResponse;
import com.undercontroll.infrastructure.logging.LogTiming;
import com.undercontroll.domain.gateway.StorageGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class S3StorageAdapter implements StorageGateway {

    private final S3Client s3Client;

    @Override
    public void putObject(String bucket, String key, byte[] data, Optional<String> contentType) {
        long started = System.nanoTime();
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .contentType(contentType.orElse(null))
                    .key(key)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(data));
            log.info("Object {} created in bucket {} durationMs={}", key, bucket, LogTiming.millisSince(started));

        } catch (S3Exception e) {
            log.error("Error while putting object key={} bucket={} durationMs={}", key, bucket, LogTiming.millisSince(started), e);
        }
    }

    @Override
    public GenerateUploadUrlResponse generatePresignedUploadUrl(String bucket, String key, Integer expirationMinutes) {
        long started = System.nanoTime();
        try (S3Presigner presigner = buildPresigner()) {
            PutObjectPresignRequest presignedRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .putObjectRequest(builder -> builder.bucket(bucket).key(key))
                    .build();

            var presignedPutRequest = presigner.presignPutObject(presignedRequest);
            String presignedUrl = presignedPutRequest.url().toString();

            log.info(
                    "Generated presigned upload URL key={} bucket={} expirationMinutes={} durationMs={}",
                    key,
                    bucket,
                    expirationMinutes,
                    LogTiming.millisSince(started)
            );

            return new GenerateUploadUrlResponse(
                    presignedUrl,
                    key,
                    System.currentTimeMillis() + (expirationMinutes * 60 * 1000L)
            );

        } catch (S3Exception e) {
            log.error(
                    "Error generating presigned upload URL key={} bucket={} durationMs={}",
                    key,
                    bucket,
                    LogTiming.millisSince(started),
                    e
            );
            throw new RuntimeException("Failed to generate presigned upload URL: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateReadPresignedUrl(String bucket, String key, long expirationMinutes) {
        long started = System.nanoTime();
        try (S3Presigner presigner = buildPresigner()) {
            GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(builder -> builder.bucket(bucket).key(key))
                    .build();

            final var result = presigner.presignGetObject(getObjectPresignRequest);

            final var url = result.url();
            log.info(
                    "Generated presigned read URL key={} bucket={} expirationMinutes={} durationMs={}",
                    key,
                    bucket,
                    expirationMinutes,
                    LogTiming.millisSince(started)
            );
            return url == null ? null : url.toString();
        } catch (S3Exception e) {
            log.error(
                    "Error while generating presigned read URL key={} bucket={} durationMs={}",
                    key,
                    bucket,
                    LogTiming.millisSince(started),
                    e
            );

            throw new RuntimeException("Failed to generate presigned read URL: " + e.getMessage(), e);
        }
    }

    private S3Presigner buildPresigner() {
        return S3Presigner.builder()
                .region(s3Client.serviceClientConfiguration().region())
                .credentialsProvider(s3Client.serviceClientConfiguration().credentialsProvider())
                .build();
    }
}
