package com.undercontroll.domain.usecase.storage;

import com.undercontroll.domain.exception.InvalidUploadException;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.gateway.ObjectStorageGateway;
import com.undercontroll.domain.gateway.UploadIntentGateway;
import com.undercontroll.domain.storage.PresignedUploadUrl;
import com.undercontroll.domain.storage.StorageArea;
import com.undercontroll.domain.storage.StoredObject;
import com.undercontroll.domain.storage.UploadIntent;
import com.undercontroll.domain.storage.UploadTicket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StorageUploadServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-08T12:00:00Z");
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0};

    private ObjectStorageGateway storage;
    private UploadIntentGateway intents;
    private StorageUploadService service;

    @BeforeEach
    void setUp() {
        storage = mock(ObjectStorageGateway.class);
        intents = mock(UploadIntentGateway.class);
        CurrentUserIdPort user = () -> 7;
        service = new StorageUploadService(storage, intents, user, Clock.fixed(NOW, ZoneOffset.UTC));
        when(storage.presignUpload(any(), anyString(), anyString(), anyLong())).thenAnswer(invocation -> new PresignedUploadUrl(
                "http://minio/x", "PUT", Map.of("Content-Type", invocation.getArgument(2)), invocation.getArgument(1), NOW.plusSeconds(300)));
        when(intents.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private UploadIntent intent(UploadIntent.Status status, Instant expiresAt, Integer owner) {
        return new UploadIntent(UUID.randomUUID(), StorageArea.ORDER_PHOTOS, "order-photos/12/a.jpg", owner, "12", "DEVICE",
                null, "image/jpeg", 6, status, expiresAt, NOW, null);
    }

    @Test
    @DisplayName("request validates type and size against the area policy and generates the key on the server")
    void request() {
        UploadTicket ticket = service.request(StorageArea.ORDER_PHOTOS, "image/jpeg; charset=x", 6L, "../../etc/passwd", "12", "DEVICE");

        ArgumentCaptor<UploadIntent> saved = ArgumentCaptor.forClass(UploadIntent.class);
        verify(intents).save(saved.capture());
        String key = saved.getValue().objectKey();
        assertTrue(key.matches("order-photos/12/[0-9a-f-]{36}\\.jpg"), key);
        assertFalse(key.contains("passwd"));
        assertEquals(UploadIntent.Status.PENDING, saved.getValue().status());
        assertEquals(7, saved.getValue().ownerUserId());
        assertEquals(ticket.uploadId(), saved.getValue().id());

        assertThrows(InvalidUploadException.class, () -> service.request(StorageArea.ORDER_PHOTOS, "application/pdf", 6L, null, "12", null));
        assertThrows(InvalidUploadException.class, () -> service.request(StorageArea.ORDER_PHOTOS, "image/heic", 6L, null, "12", null));
        assertThrows(InvalidUploadException.class, () -> service.request(StorageArea.ORDER_PHOTOS, "image/jpeg", 0L, null, "12", null));
        assertThrows(InvalidUploadException.class, () -> service.request(StorageArea.ORDER_PHOTOS, "image/jpeg", 11L * 1024 * 1024, null, "12", null));
        assertThrows(IllegalArgumentException.class, () -> service.request(StorageArea.ORDER_PHOTOS, "image/jpeg", 6L, null, "../x", null));
    }

    @Test
    @DisplayName("complete checks the stored object and marks it completed; a repeat is idempotent")
    void complete() {
        UploadIntent pending = intent(UploadIntent.Status.PENDING, NOW.plusSeconds(60), 7);
        when(intents.findById(pending.id())).thenReturn(Optional.of(pending));
        when(storage.stat(StorageArea.ORDER_PHOTOS, "order-photos/12/a.jpg")).thenReturn(Optional.of(new StoredObject(6, "image/jpeg")));
        when(storage.readHead(eq(StorageArea.ORDER_PHOTOS), eq("order-photos/12/a.jpg"), anyInt())).thenReturn(JPEG);

        UploadIntent completed = service.complete(pending.id(), StorageArea.ORDER_PHOTOS, "12");

        assertEquals(UploadIntent.Status.COMPLETED, completed.status());
        verify(intents).markCompleted(pending.id(), NOW);

        UploadIntent done = intent(UploadIntent.Status.COMPLETED, NOW, 7);
        when(intents.findById(done.id())).thenReturn(Optional.of(done));
        assertEquals(done, service.complete(done.id(), StorageArea.ORDER_PHOTOS, "12"));
    }

    @Test
    @DisplayName("wrong owner, area or reference looks like an unknown upload")
    void foreignUpload() {
        UploadIntent other = intent(UploadIntent.Status.PENDING, NOW.plusSeconds(60), 99);
        when(intents.findById(other.id())).thenReturn(Optional.of(other));
        UploadIntent mine = intent(UploadIntent.Status.PENDING, NOW.plusSeconds(60), 7);
        when(intents.findById(mine.id())).thenReturn(Optional.of(mine));

        assertThrows(InvalidUploadException.class, () -> service.complete(other.id(), StorageArea.ORDER_PHOTOS, "12"));
        assertThrows(InvalidUploadException.class, () -> service.complete(mine.id(), StorageArea.LABEL_SCANS, null));
        assertThrows(InvalidUploadException.class, () -> service.complete(mine.id(), StorageArea.ORDER_PHOTOS, "13"));
        verify(storage, never()).stat(any(), any());
    }

    @Test
    @DisplayName("a size or content mismatch deletes the object and rejects the upload")
    void rejects() {
        UploadIntent sizeMismatch = intent(UploadIntent.Status.PENDING, NOW.plusSeconds(60), 7);
        when(intents.findById(sizeMismatch.id())).thenReturn(Optional.of(sizeMismatch));
        when(storage.stat(StorageArea.ORDER_PHOTOS, "order-photos/12/a.jpg")).thenReturn(Optional.of(new StoredObject(9, "image/jpeg")));

        assertThrows(InvalidUploadException.class, () -> service.complete(sizeMismatch.id(), StorageArea.ORDER_PHOTOS, "12"));
        verify(storage).delete(StorageArea.ORDER_PHOTOS, "order-photos/12/a.jpg");
        verify(intents).markRejected(sizeMismatch.id(), NOW);

        UploadIntent fake = intent(UploadIntent.Status.PENDING, NOW.plusSeconds(60), 7);
        when(intents.findById(fake.id())).thenReturn(Optional.of(fake));
        when(storage.stat(StorageArea.ORDER_PHOTOS, "order-photos/12/a.jpg")).thenReturn(Optional.of(new StoredObject(6, "image/jpeg")));
        when(storage.readHead(eq(StorageArea.ORDER_PHOTOS), any(), anyInt())).thenReturn("%PDF-1".getBytes());
        assertThrows(InvalidUploadException.class, () -> service.complete(fake.id(), StorageArea.ORDER_PHOTOS, "12"));
        verify(intents).markRejected(fake.id(), NOW);
    }

    @Test
    @DisplayName("not uploaded yet is a retryable 400; after expiry it is rejected")
    void notUploaded() {
        UploadIntent early = intent(UploadIntent.Status.PENDING, NOW.plusSeconds(60), 7);
        UploadIntent late = intent(UploadIntent.Status.PENDING, NOW.minusSeconds(1), 7);
        when(intents.findById(early.id())).thenReturn(Optional.of(early));
        when(intents.findById(late.id())).thenReturn(Optional.of(late));
        when(storage.stat(any(), any())).thenReturn(Optional.empty());

        assertThrows(InvalidUploadException.class, () -> service.complete(early.id(), StorageArea.ORDER_PHOTOS, "12"));
        verify(intents, never()).markRejected(eq(early.id()), any());
        assertThrows(InvalidUploadException.class, () -> service.complete(late.id(), StorageArea.ORDER_PHOTOS, "12"));
        verify(intents).markRejected(late.id(), NOW);
    }
}
