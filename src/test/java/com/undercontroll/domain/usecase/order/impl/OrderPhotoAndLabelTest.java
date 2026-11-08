package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.domain.enums.OrderPhotoKind;
import com.undercontroll.domain.exception.InvalidLabelReadingException;
import com.undercontroll.domain.exception.InvalidOrderPhotoException;
import com.undercontroll.domain.exception.InvalidUploadException;
import com.undercontroll.domain.exception.LabelReadingUnavailableException;
import com.undercontroll.domain.exception.OrderPhotoLimitException;
import com.undercontroll.domain.exception.OrderPhotoNotFoundException;
import com.undercontroll.domain.gateway.LabelExtractionGateway;
import com.undercontroll.domain.gateway.ObjectStorageGateway;
import com.undercontroll.domain.gateway.OrderPhotoGateway;
import com.undercontroll.domain.gateway.UploadIntentGateway;
import com.undercontroll.domain.model.LabelReading;
import com.undercontroll.domain.model.Order;
import com.undercontroll.domain.model.OrderPhoto;
import com.undercontroll.domain.storage.StorageArea;
import com.undercontroll.domain.storage.UploadIntent;
import com.undercontroll.domain.storage.UploadTicket;
import com.undercontroll.domain.usecase.order.RequestOrderPhotoUploadPort;
import com.undercontroll.domain.usecase.storage.StorageUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderPhotoAndLabelTest {

    private static final Instant NOW = Instant.parse("2026-11-08T12:00:00Z");
    private static final UUID UPLOAD = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");

    private OrderAccess access;
    private OrderPhotoGateway photos;
    private UploadIntentGateway intents;
    private StorageUploadService uploads;
    private ObjectStorageGateway storage;
    private OrderPhotoUseCases useCases;

    @BeforeEach
    void setUp() {
        access = mock(OrderAccess.class);
        photos = mock(OrderPhotoGateway.class);
        intents = mock(UploadIntentGateway.class);
        uploads = mock(StorageUploadService.class);
        storage = mock(ObjectStorageGateway.class);
        OrderDetailsAssembler assembler = new OrderDetailsAssembler(null, photos, storage, null);
        useCases = new OrderPhotoUseCases(access, photos, intents, uploads, storage, assembler, Clock.fixed(NOW, ZoneOffset.UTC));
        when(access.require(1)).thenReturn(Order.builder().id(1).build());
        when(photos.save(any())).thenAnswer(invocation -> {
            OrderPhoto photo = invocation.getArgument(0);
            return new OrderPhoto(9L, photo.orderId(), photo.kind(), photo.storageKey(), photo.contentType(), photo.size(), photo.createdAt());
        });
        when(storage.presignRead(eq(StorageArea.ORDER_PHOTOS), any())).thenReturn("https://minio/signed");
    }

    private static UploadIntent completed(StorageArea area, String key, String kind) {
        return new UploadIntent(UPLOAD, area, key, 7, "1", kind, null, "image/png", 6, UploadIntent.Status.COMPLETED, NOW, NOW, NOW);
    }

    @Test
    @DisplayName("photo upload ticket is scoped to the order and counts pending uploads in the limit of 6")
    void requestPhoto() {
        UploadTicket ticket = mock(UploadTicket.class);
        when(uploads.request(StorageArea.ORDER_PHOTOS, "image/png", 6L, null, "1", "DEVICE")).thenReturn(ticket);
        when(photos.countByOrderId(1)).thenReturn(3L);
        when(intents.countPending(StorageArea.ORDER_PHOTOS, "1", NOW)).thenReturn(2L);

        assertSame(ticket, useCases.execute(new RequestOrderPhotoUploadPort.Input(1, OrderPhotoKind.DEVICE, "image/png", 6L)));

        when(intents.countPending(StorageArea.ORDER_PHOTOS, "1", NOW)).thenReturn(3L);
        assertThrows(OrderPhotoLimitException.class,
                () -> useCases.execute(new RequestOrderPhotoUploadPort.Input(1, OrderPhotoKind.DEVICE, "image/png", 6L)));
        assertThrows(InvalidOrderPhotoException.class,
                () -> useCases.execute(new RequestOrderPhotoUploadPort.Input(1, null, "image/png", 6L)));
    }

    @Test
    @DisplayName("completing stores the photo once and returns a signed URL; repeating returns the same photo")
    void completePhoto() {
        when(uploads.complete(UPLOAD, StorageArea.ORDER_PHOTOS, "1")).thenReturn(completed(StorageArea.ORDER_PHOTOS, "order-photos/1/a.png", "LABEL"));
        when(photos.findByStorageKey("order-photos/1/a.png")).thenReturn(Optional.empty())
                .thenReturn(Optional.of(new OrderPhoto(9L, 1, OrderPhotoKind.LABEL, "order-photos/1/a.png", "image/png", 6, NOW)));

        var first = useCases.execute(1, UPLOAD);
        var second = useCases.execute(1, UPLOAD);

        assertEquals(9L, first.id());
        assertEquals(OrderPhotoKind.LABEL, first.kind());
        assertEquals("https://minio/signed", first.url());
        assertEquals(first.id(), second.id());
        verify(photos, times(1)).save(any());
    }

    @Test
    @DisplayName("delete removes the row and the object; unknown photo is 404")
    void deletes() {
        when(photos.findById(1, 9L)).thenReturn(Optional.of(new OrderPhoto(9L, 1, OrderPhotoKind.DEVICE, "order-photos/1/a.jpg", "image/jpeg", 5, NOW)));

        useCases.execute(1, 9L);

        verify(photos).delete(9L);
        verify(storage).delete(StorageArea.ORDER_PHOTOS, "order-photos/1/a.jpg");
        assertThrows(OrderPhotoNotFoundException.class, () -> useCases.execute(1, 10L));
    }

    @Test
    @DisplayName("label reading reads the uploaded object, normalizes voltage and maps failures")
    @SuppressWarnings("unchecked")
    void labelReading() {
        ObjectProvider<LabelExtractionGateway> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(image -> new LabelReading(true, " liquidificador ", "Arno", "LN50", "SN1", "110 v", 1.4));
        ReadApplianceLabelImpl reader = new ReadApplianceLabelImpl(provider, uploads, storage);
        UploadIntent scan = new UploadIntent(UPLOAD, StorageArea.LABEL_SCANS, "label-scans/7/a.png", 7, null, null, null,
                "image/png", 6, UploadIntent.Status.COMPLETED, NOW, NOW, NOW);
        when(uploads.complete(UPLOAD, StorageArea.LABEL_SCANS, null)).thenReturn(scan);
        when(storage.read(eq(StorageArea.LABEL_SCANS), eq("label-scans/7/a.png"), anyLong())).thenReturn(new byte[]{1});

        LabelReading reading = reader.execute(UPLOAD);
        assertEquals("liquidificador", reading.type());
        assertEquals("127V", reading.voltage());
        assertEquals(1.0, reading.confidence());

        when(provider.getIfAvailable()).thenReturn(image -> new LabelReading(false, "x", "y", "z", "w", "220V", 0.2));
        LabelReading notLabel = reader.execute(UPLOAD);
        assertFalse(notLabel.isLabel());
        assertNull(notLabel.brand());

        when(provider.getIfAvailable()).thenReturn(image -> {
            throw new IllegalStateException("timeout");
        });
        assertThrows(LabelReadingUnavailableException.class, () -> reader.execute(UPLOAD));

        when(provider.getIfAvailable()).thenReturn(image -> new LabelReading(true, null, null, null, null, null, 0.5));
        when(uploads.complete(UPLOAD, StorageArea.LABEL_SCANS, null)).thenThrow(new InvalidUploadException("Upload não encontrado"));
        assertThrows(InvalidLabelReadingException.class, () -> reader.execute(UPLOAD));

        when(provider.getIfAvailable()).thenReturn(null);
        assertThrows(LabelReadingUnavailableException.class, () -> reader.execute(UPLOAD));
        assertThrows(LabelReadingUnavailableException.class, () -> reader.execute("image/png", 10L));
        verify(uploads, never()).request(eq(StorageArea.LABEL_SCANS), any(), any(), any(), any(), any());
    }
}
