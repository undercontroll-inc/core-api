package com.undercontroll.application.usecase.stockimport;

import com.undercontroll.domain.enums.StockImportSourceType;
import com.undercontroll.domain.enums.StockImportStage;
import com.undercontroll.domain.enums.StockImportStatus;
import com.undercontroll.domain.exception.InvalidStockImportException;
import com.undercontroll.domain.exception.StockImportUnavailableException;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.gateway.InvoiceExtractionGateway;
import com.undercontroll.domain.gateway.PdfInspectorGateway;
import com.undercontroll.domain.gateway.StockImportDispatcher;
import com.undercontroll.domain.gateway.StockImportGateway;
import com.undercontroll.domain.model.stockimport.InvoiceUpload;
import com.undercontroll.domain.model.stockimport.StockImport;
import com.undercontroll.domain.usecase.stockimport.impl.CreateStockImportImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateStockImportImplTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
    private static final byte[] PDF = "%PDF-1.7\n...".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] XML = "﻿  <nfeProc/>".getBytes(StandardCharsets.UTF_8);

    private StockImportGateway gateway;
    private ObjectProvider<InvoiceExtractionGateway> ai;
    private PdfInspectorGateway pdf;
    private StockImportDispatcher dispatcher;
    private CreateStockImportImpl create;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        gateway = mock(StockImportGateway.class);
        ai = mock(ObjectProvider.class);
        when(ai.getIfAvailable()).thenReturn(mock(InvoiceExtractionGateway.class));
        pdf = mock(PdfInspectorGateway.class);
        dispatcher = mock(StockImportDispatcher.class);
        CurrentUserIdPort user = () -> 12;
        create = new CreateStockImportImpl(gateway, ai, pdf, dispatcher, user,
                Clock.fixed(Instant.parse("2026-11-06T12:00:00Z"), ZoneOffset.UTC));
        when(gateway.create(any(), any())).thenAnswer(invocation ->
                ((StockImport) invocation.getArgument(0)).toBuilder().id(1L).build());
    }

    @Test
    @DisplayName("stores a PROCESSING import and dispatches the worker")
    void createsImage() {
        StockImport created = create.execute(new InvoiceUpload(JPEG, "image/jpeg", "nota.jpg"));

        ArgumentCaptor<StockImport> captor = ArgumentCaptor.forClass(StockImport.class);
        verify(gateway).create(captor.capture(), eq(JPEG));
        assertEquals(StockImportStatus.PROCESSING, captor.getValue().status());
        assertEquals(StockImportStage.RECEIVED, captor.getValue().stage());
        assertEquals(StockImportSourceType.IMAGE, captor.getValue().sourceType());
        assertEquals(12, captor.getValue().createdBy());
        assertEquals(64, captor.getValue().fileSha256().length());
        assertEquals(1L, created.id());
        verify(dispatcher).dispatch();
    }

    @Test
    @DisplayName("XML is accepted even with the AI disabled; images are not")
    void aiDisabled() {
        when(ai.getIfAvailable()).thenReturn(null);

        assertEquals(StockImportSourceType.NFE_XML,
                create.execute(new InvoiceUpload(XML, "text/xml", "nfe.xml")).sourceType());
        assertThrows(StockImportUnavailableException.class,
                () -> create.execute(new InvoiceUpload(JPEG, "image/jpeg", "nota.jpg")));
    }

    @Test
    @DisplayName("rejects empty, unknown, mismatched, HEIC and long PDF uploads")
    void rejects() {
        assertThrows(InvalidStockImportException.class, () -> create.execute(new InvoiceUpload(new byte[0], "image/jpeg", "a")));
        assertThrows(InvalidStockImportException.class, () -> create.execute(new InvoiceUpload("hello".getBytes(), null, "a")));
        assertThrows(InvalidStockImportException.class, () -> create.execute(new InvoiceUpload(JPEG, "application/pdf", "a")));
        assertThrows(InvalidStockImportException.class, () -> create.execute(new InvoiceUpload(JPEG, "image/heic", "a")));
        when(pdf.pageCount(PDF)).thenReturn(OptionalInt.of(6));
        assertThrows(InvalidStockImportException.class, () -> create.execute(new InvoiceUpload(PDF, "application/pdf", "a")));
        when(pdf.pageCount(PDF)).thenReturn(OptionalInt.empty());
        assertThrows(InvalidStockImportException.class, () -> create.execute(new InvoiceUpload(PDF, null, "a")));
        verify(gateway, never()).create(any(), any());
    }

    @Test
    @DisplayName("octet-stream falls back to content detection")
    void octetStream() {
        when(pdf.pageCount(PDF)).thenReturn(OptionalInt.of(2));

        assertEquals(StockImportSourceType.PDF,
                create.execute(new InvoiceUpload(PDF, "application/octet-stream", "nota.pdf")).sourceType());
    }
}
