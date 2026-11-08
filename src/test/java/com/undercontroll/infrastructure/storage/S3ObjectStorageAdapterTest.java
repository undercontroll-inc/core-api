package com.undercontroll.infrastructure.storage;

import com.undercontroll.domain.exception.StorageUnavailableException;
import com.undercontroll.domain.storage.PresignedUploadUrl;
import com.undercontroll.domain.storage.StorageArea;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class S3ObjectStorageAdapterTest {

    private final S3Client s3 = mock(S3Client.class);
    private final S3Presigner presigner = S3Presigner.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("minioadmin", "minioadmin")))
            .endpointOverride(URI.create("http://192.168.0.10:9000"))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();
    private final S3ObjectStorageAdapter adapter = new S3ObjectStorageAdapter(
            s3, presigner, Clock.fixed(Instant.parse("2026-11-08T12:00:00Z"), ZoneOffset.UTC),
            "undercontroll-uploads", "undercontroll-exports", true);

    @AfterEach
    void close() {
        presigner.close();
    }

    @Test
    @DisplayName("upload URL signs content type, length and encryption on the public host with the policy TTL")
    void presignUpload() {
        PresignedUploadUrl upload = adapter.presignUpload(StorageArea.ORDER_PHOTOS, "order-photos/12/a.jpg", "image/jpeg", 183442);
        String url = URLDecoder.decode(upload.url(), StandardCharsets.UTF_8);

        assertThat(url).startsWith("http://192.168.0.10:9000/undercontroll-uploads/order-photos/12/a.jpg?");
        assertThat(url).contains("X-Amz-Expires=300");
        assertThat(url).contains("X-Amz-SignedHeaders=content-length;content-type;host;x-amz-server-side-encryption");
        assertThat(upload.method()).isEqualTo("PUT");
        assertThat(upload.headers()).containsEntry("Content-Type", "image/jpeg")
                .containsEntry("Content-Length", "183442")
                .containsEntry("x-amz-server-side-encryption", "AES256")
                .doesNotContainKey("host");
    }

    @Test
    @DisplayName("read URL carries Cache-Control, uses the area TTL and is reused while more than half valid")
    void presignRead() {
        String first = adapter.presignRead(StorageArea.ORDER_PHOTOS, "order-photos/12/a.jpg");
        String second = adapter.presignRead(StorageArea.ORDER_PHOTOS, "order-photos/12/a.jpg");
        String decoded = URLDecoder.decode(first, StandardCharsets.UTF_8);

        assertThat(second).isSameAs(first);
        assertThat(decoded).contains("X-Amz-Expires=900");
        assertThat(decoded).contains("response-cache-control=private, max-age=450, immutable");
        assertThat(decoded).startsWith("http://192.168.0.10:9000/undercontroll-uploads/");

        String export = URLDecoder.decode(adapter.presignRead(StorageArea.ORDER_EXPORTS, "order-exports/12.pdf"), StandardCharsets.UTF_8);
        assertThat(export).startsWith("http://192.168.0.10:9000/undercontroll-exports/order-exports/12.pdf?");
        assertThat(export).contains("response-content-disposition=attachment; filename=\"12.pdf\"");
    }

    @Test
    @DisplayName("keys outside the area prefix, path traversal and server-only areas are refused")
    void guards() {
        assertThatThrownBy(() -> adapter.presignRead(StorageArea.ORDER_PHOTOS, "announcements/1/a.png"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.presignRead(StorageArea.ORDER_PHOTOS, "order-photos/../x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.presignRead(StorageArea.LABEL_SCANS, "label-scans/1/a.jpg"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> adapter.presignUpload(StorageArea.ORDER_EXPORTS, "order-exports/1.pdf", "application/pdf", 10))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("stat maps missing objects to empty and other failures to STORAGE_UNAVAILABLE")
    void stat() {
        when(s3.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentLength(10L).contentType("image/png").build())
                .thenThrow(NoSuchKeyException.builder().build())
                .thenThrow(S3Exception.builder().statusCode(500).build());

        assertThat(adapter.stat(StorageArea.ORDER_PHOTOS, "order-photos/1/a.png")).hasValueSatisfying(object -> {
            assertThat(object.size()).isEqualTo(10L);
            assertThat(object.contentType()).isEqualTo("image/png");
        });
        assertThat(adapter.stat(StorageArea.ORDER_PHOTOS, "order-photos/1/b.png")).isEqualTo(Optional.empty());
        assertThatThrownBy(() -> adapter.stat(StorageArea.ORDER_PHOTOS, "order-photos/1/c.png"))
                .isInstanceOf(StorageUnavailableException.class);
    }

    @Test
    @DisplayName("server-side put failures are reported, never swallowed")
    void putFails() {
        when(s3.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenThrow(S3Exception.builder().statusCode(500).build());

        assertThatThrownBy(() -> adapter.put(StorageArea.ORDER_EXPORTS, "order-exports/1.pdf", new byte[]{1}, "application/pdf"))
                .isInstanceOf(StorageUnavailableException.class);
    }
}
