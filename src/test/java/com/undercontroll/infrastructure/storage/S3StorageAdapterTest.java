package com.undercontroll.infrastructure.storage;

import com.undercontroll.domain.model.PresignedUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3StorageAdapterTest {

    private final S3Client s3Client = mock(S3Client.class);
    private final S3Presigner presigner = mock(S3Presigner.class);
    private S3StorageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = spy(new S3StorageAdapter(s3Client, "uploads", "exports", 15, 1440L));
        doReturn(presigner).when(adapter).buildPresigner();
    }

    @Test
    @DisplayName("presigns announcement uploads in the upload bucket with the configured expiration")
    void presignsUpload() throws Exception {
        PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("https://s3.example/put").toURL());
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presigned);

        PresignedUpload upload = adapter.presignAnnouncementImageUpload("announcements/1/a.png");

        ArgumentCaptor<PutObjectPresignRequest> request = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(presigner).presignPutObject(request.capture());
        assertThat(request.getValue().putObjectRequest().bucket()).isEqualTo("uploads");
        assertThat(request.getValue().signatureDuration()).isEqualTo(Duration.ofMinutes(15));
        assertThat(upload.presignedUrl()).isEqualTo("https://s3.example/put");
        assertThat(upload.fileKey()).isEqualTo("announcements/1/a.png");
        assertThat(upload.expirationTime()).isGreaterThan(System.currentTimeMillis());
    }

    @Test
    @DisplayName("presigns reads in the upload bucket with the read expiration")
    void presignsRead() throws Exception {
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("https://s3.example/get").toURL());
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        assertThat(adapter.announcementImageReadUrl("announcements/1/a.png")).isEqualTo("https://s3.example/get");

        ArgumentCaptor<GetObjectPresignRequest> request = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(presigner).presignGetObject(request.capture());
        assertThat(request.getValue().getObjectRequest().bucket()).isEqualTo("uploads");
        assertThat(request.getValue().signatureDuration()).isEqualTo(Duration.ofDays(1));
    }

    @Test
    @DisplayName("stores order exports as PDF in the export bucket and swallows S3 errors")
    void storesExport() {
        adapter.storeOrderExport("12", new byte[]{1});

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        assertThat(request.getValue().bucket()).isEqualTo("exports");
        assertThat(request.getValue().contentType()).isEqualTo("application/pdf");

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("down").build());
        adapter.storeOrderExport("13", new byte[]{1});
    }

    @Test
    @DisplayName("presign failures surface as runtime errors")
    void presignFailures() {
        when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenThrow(S3Exception.builder().message("denied").build());
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenThrow(S3Exception.builder().message("denied").build());

        assertThatThrownBy(() -> adapter.presignAnnouncementImageUpload("k")).hasMessageContaining("presigned upload URL");
        assertThatThrownBy(() -> adapter.announcementImageReadUrl("k")).hasMessageContaining("presigned read URL");
    }
}
