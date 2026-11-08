package com.undercontroll.infrastructure.storage;

import com.undercontroll.domain.exception.InvalidUploadException;
import com.undercontroll.domain.exception.StorageUnavailableException;
import com.undercontroll.domain.gateway.ObjectStorageGateway;
import com.undercontroll.domain.storage.PresignedUploadUrl;
import com.undercontroll.domain.storage.StorageArea;
import com.undercontroll.domain.storage.StorageBucket;
import com.undercontroll.domain.storage.StoredObject;
import com.undercontroll.domain.support.Durations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class S3ObjectStorageAdapter implements ObjectStorageGateway {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final PresignedReadCache readCache;
    private final String uploadsBucket;
    private final String exportsBucket;
    private final boolean serverSideEncryption;

    public S3ObjectStorageAdapter(
            S3Client s3,
            S3Presigner presigner,
            Clock clock,
            @Value("${undercontroll.storage.buckets.uploads}") String uploadsBucket,
            @Value("${undercontroll.storage.buckets.exports}") String exportsBucket,
            @Value("${undercontroll.storage.server-side-encryption:true}") boolean serverSideEncryption
    ) {
        this.s3 = s3;
        this.presigner = presigner;
        this.readCache = new PresignedReadCache(clock, 50_000);
        this.uploadsBucket = uploadsBucket;
        this.exportsBucket = exportsBucket;
        this.serverSideEncryption = serverSideEncryption;
    }

    @Override
    public PresignedUploadUrl presignUpload(StorageArea area, String key, String contentType, long sizeBytes) {
        requireOwned(area, key);
        if (!area.clientWritable()) {
            throw new IllegalStateException(area + " does not accept client uploads");
        }
        PutObjectRequest.Builder put = PutObjectRequest.builder()
                .bucket(bucket(area))
                .key(key)
                .contentType(contentType)
                .contentLength(sizeBytes);
        if (serverSideEncryption) {
            put.serverSideEncryption(ServerSideEncryption.AES256);
        }
        try {
            PresignedPutObjectRequest presigned = presigner.presignPutObject(request -> request
                    .signatureDuration(area.policy().uploadTtl())
                    .putObjectRequest(put.build()));
            return new PresignedUploadUrl(presigned.url().toString(), "PUT", clientHeaders(presigned.signedHeaders()),
                    key, presigned.expiration());
        } catch (RuntimeException ex) {
            throw new StorageUnavailableException("Could not create the upload URL", ex);
        }
    }

    @Override
    public String presignRead(StorageArea area, String key) {
        requireOwned(area, key);
        if (!area.clientReadable()) {
            throw new IllegalStateException(area + " is not readable by clients");
        }
        Duration ttl = area.policy().readTtl();
        return readCache.get(area, key, () -> {
            GetObjectRequest.Builder get = GetObjectRequest.builder()
                    .bucket(bucket(area))
                    .key(key)
                    .responseCacheControl("private, max-age=" + ttl.dividedBy(2).toSeconds() + ", immutable");
            if (area.policy().attachment()) {
                get.responseContentDisposition("attachment; filename=\"" + key.substring(key.lastIndexOf('/') + 1) + "\"");
            }
            try {
                return presigner.presignGetObject(request -> request
                        .signatureDuration(ttl)
                        .getObjectRequest(get.build())).url().toString();
            } catch (RuntimeException ex) {
                throw new StorageUnavailableException("Could not create the read URL", ex);
            }
        });
    }

    @Override
    public Optional<StoredObject> stat(StorageArea area, String key) {
        requireOwned(area, key);
        try {
            HeadObjectResponse head = s3.headObject(HeadObjectRequest.builder().bucket(bucket(area)).key(key).build());
            return Optional.of(new StoredObject(head.contentLength(), head.contentType()));
        } catch (NoSuchKeyException ex) {
            return Optional.empty();
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                return Optional.empty();
            }
            throw new StorageUnavailableException("Could not read object metadata", ex);
        } catch (RuntimeException ex) {
            throw new StorageUnavailableException("Could not read object metadata", ex);
        }
    }

    @Override
    public byte[] readHead(StorageArea area, String key, int bytes) {
        requireOwned(area, key);
        try {
            return s3.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket(area))
                    .key(key)
                    .range("bytes=0-" + (bytes - 1))
                    .build()).asByteArray();
        } catch (RuntimeException ex) {
            throw new StorageUnavailableException("Could not read the object", ex);
        }
    }

    @Override
    public byte[] read(StorageArea area, String key, long maxBytes) {
        StoredObject object = stat(area, key).orElseThrow(() -> new InvalidUploadException("Arquivo não encontrado no storage"));
        if (object.size() > maxBytes) {
            throw new InvalidUploadException("Arquivo maior que o permitido");
        }
        long started = System.nanoTime();
        try {
            byte[] content = s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket(area)).key(key).build()).asByteArray();
            log.info("Object read area={} bytes={} durationMs={}", area, content.length, Durations.millisSince(started));
            return content;
        } catch (RuntimeException ex) {
            throw new StorageUnavailableException("Could not read the object", ex);
        }
    }

    @Override
    public void put(StorageArea area, String key, byte[] content, String contentType) {
        requireOwned(area, key);
        PutObjectRequest.Builder put = PutObjectRequest.builder()
                .bucket(bucket(area))
                .key(key)
                .contentType(contentType);
        if (serverSideEncryption) {
            put.serverSideEncryption(ServerSideEncryption.AES256);
        }
        try {
            s3.putObject(put.build(), RequestBody.fromBytes(content));
        } catch (RuntimeException ex) {
            throw new StorageUnavailableException("Could not store the object", ex);
        }
    }

    @Override
    public void delete(StorageArea area, String key) {
        requireOwned(area, key);
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket(area)).key(key).build());
        } catch (RuntimeException ex) {
            log.warn("Object delete failed area={} cause={}", area, ex.toString());
        }
    }

    String bucket(StorageArea area) {
        return area.policy().bucket() == StorageBucket.EXPORTS ? exportsBucket : uploadsBucket;
    }

    static Map<String, String> clientHeaders(Map<String, List<String>> signed) {
        Map<String, String> headers = new LinkedHashMap<>();
        signed.forEach((name, values) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            if (!"host".equals(lower)) {
                headers.put(canonical(lower), String.join(",", values));
            }
        });
        return headers;
    }

    static String canonical(String lower) {
        if (lower.startsWith("x-amz-")) {
            return lower;
        }
        StringBuilder name = new StringBuilder(lower.length());
        for (String part : lower.split("-")) {
            if (!name.isEmpty()) {
                name.append('-');
            }
            name.append(part.isEmpty() ? part : Character.toUpperCase(part.charAt(0)) + part.substring(1));
        }
        return name.toString();
    }

    private static void requireOwned(StorageArea area, String key) {
        if (!area.owns(key)) {
            throw new IllegalArgumentException("Key does not belong to " + area);
        }
    }
}
