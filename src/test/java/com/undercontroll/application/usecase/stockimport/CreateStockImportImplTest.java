package com.undercontroll.application.usecase.stockimport;

import com.undercontroll.domain.enums.StockImportSourceType;
import com.undercontroll.domain.enums.StockImportStage;
import com.undercontroll.domain.enums.StockImportStatus;
import com.undercontroll.domain.exception.InvalidStockImportException;
import com.undercontroll.domain.exception.InvalidUploadException;
import com.undercontroll.domain.exception.StockImportUnavailableException;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.gateway.InvoiceExtractionGateway;
import com.undercontroll.domain.gateway.ObjectStorageGateway;
import com.undercontroll.domain.gateway.PdfInspectorGateway;
import com.undercontroll.domain.gateway.StockImportDispatcher;
import com.undercontroll.domain.gateway.StockImportGateway;
import com.undercontroll.domain.model.stockimport.StockImport;
import com.undercontroll.domain.storage.StorageArea;
import com.undercontroll.domain.storage.UploadIntent;
import com.undercontroll.domain.storage.UploadTicket;
import com.undercontroll.domain.usecase.stockimport.RequestStockImportUploadPort;
import com.undercontroll.domain.usecase.stockimport.impl.CreateStockImportImpl;
import com.undercontroll.domain.usecase.storage.StorageUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateStockImportImplTest {

    private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");
    private static final UUID UPLOAD = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
    private static final byte[] PDF = "%PDF-1.7\n...".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] XML = "﻿  <nfeProc/>".getBytes(StandardCharsets.UTF_8);

    private StockImportGateway gateway;
    private ObjectProvider<InvoiceExtractionGateway> ai;
    private PdfInspectorGateway pdf;
    private StockImportDispatcher dispatcher;
    private StorageUploadService uploads;
    private ObjectStorageGateway storage;
    private CreateStockImportImpl create;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        gateway = mock(StockImportGateway.class);
        ai = mock(ObjectProvider.class);
        when(ai.getIfAvailable()).thenReturn(mock(InvoiceExtractionGateway.class));
        pdf = mock(PdfInspectorGateway.class);
        dispatcher = mock(StockImportDispatcher.class);
        uploads = mock(StorageUploadService.class);
        storage = mock(ObjectStorageGateway.class);
        CurrentUserIdPort user = () -> 12;
        create = new CreateStockImportImpl(gateway, ai, pdf, dispatcher, user, Clock.fixed(NOW, ZoneOffset.UTC), uploads, storage);
        when(gateway.create(any())).thenAnswer(invocation -> ((StockImport) invocation.getArgument(0)).toBuilder().id(1L).build());
        when(gateway.findByStorageKey(any())).thenReturn(Optional.empty());
    }

    private void uploaded(String contentType, byte[] content) {
        when(uploads.complete(UPLOAD, StorageArea.STOCK_IMPORTS, null)).thenReturn(new UploadIntent(UPLOAD, StorageArea.STOCK_IMPORTS,
                "stock-imports/12/a.bin", 12, null, null, "nota.jpg", contentType, content.length,
                UploadIntent.Status.COMPLETED, NOW, NOW, NOW));
        when(storage.read(eq(StorageArea.STOCK_IMPORTS), eq("stock-imports/12/a.bin"), anyLong())).thenReturn(content);
    }

    @Test
    @DisplayName("stores a PROCESSING import pointing at the uploaded object and dispatches the worker")
    void createsImage() {
        uploaded("image/jpeg", JPEG);

        StockImport created = create.execute(UPLOAD);

        ArgumentCaptor<StockImport> captor = ArgumentCaptor.forClass(StockImport.class);
        verify(gateway).create(captor.capture());
        assertEquals(StockImportStatus.PROCESSING, captor.getValue().status());
        assertEquals(StockImportStage.RECEIVED, captor.getValue().stage());
        assertEquals(StockImportSourceType.IMAGE, captor.getValue().sourceType());
        assertEquals("stock-imports/12/a.bin", captor.getValue().storageKey());
        assertEquals("nota.jpg", captor.getValue().fileName());
        assertEquals(12, captor.getValue().createdBy());
        assertEquals(64, captor.getValue().fileSha256().length());
        assertEquals(1L, created.id());
        verify(dispatcher).dispatch();
    }

    @Test
    @DisplayName("the same upload twice returns the existing import")
    void idempotent() {
        uploaded("image/jpeg", JPEG);
        StockImport existing = StockImport.builder().id(5L).build();
        when(gateway.findByStorageKey("stock-imports/12/a.bin")).thenReturn(Optional.of(existing));

        assertSame(existing, create.execute(UPLOAD));
        verify(gateway, never()).create(any());
    }

    @Test
    @DisplayName("upload request: XML is accepted with the AI disabled; images are not; invalid types map to STOCK_IMPORT_INVALID")
    void requestUpload() {
        UploadTicket ticket = mock(UploadTicket.class);
        when(uploads.request(StorageArea.STOCK_IMPORTS, "text/xml", 10L, "nfe.xml", null, null)).thenReturn(ticket);
        when(uploads.request(StorageArea.STOCK_IMPORTS, "image/heic", 10L, "a.heic", null, null)).thenThrow(new InvalidUploadException("Tipo"));
        when(ai.getIfAvailable()).thenReturn(null);

        assertSame(ticket, create.execute(new RequestStockImportUploadPort.Input("text/xml", 10L, "nfe.xml")));
        assertThrows(StockImportUnavailableException.class,
                () -> create.execute(new RequestStockImportUploadPort.Input("image/jpeg", 10L, "a.jpg")));
        when(ai.getIfAvailable()).thenReturn(mock(InvoiceExtractionGateway.class));
        assertThrows(InvalidStockImportException.class,
                () -> create.execute(new RequestStockImportUploadPort.Input("image/heic", 10L, "a.heic")));
    }

    @Test
    @DisplayName("rejects content that contradicts the declared type, long or unreadable PDFs and missing uploads")
    void rejects() {
        uploaded("application/pdf", JPEG);
        assertThrows(InvalidStockImportException.class, () -> create.execute(UPLOAD));

        uploaded("application/pdf", PDF);
        when(pdf.pageCount(PDF)).thenReturn(OptionalInt.of(6));
        assertThrows(InvalidStockImportException.class, () -> create.execute(UPLOAD));
        when(pdf.pageCount(PDF)).thenReturn(OptionalInt.empty());
        assertThrows(InvalidStockImportException.class, () -> create.execute(UPLOAD));

        when(uploads.complete(UPLOAD, StorageArea.STOCK_IMPORTS, null)).thenThrow(new InvalidUploadException("Upload não encontrado"));
        assertThrows(InvalidStockImportException.class, () -> create.execute(UPLOAD));
        assertThrows(InvalidStockImportException.class, () -> create.execute((UUID) null));
        verify(gateway, never()).create(any());
    }

    @Test
    @DisplayName("XML creates an NFE_XML import")
    void xml() {
        uploaded("text/xml", XML);

        assertEquals(StockImportSourceType.NFE_XML, create.execute(UPLOAD).sourceType());
    }
}
