package com.undercontroll.domain.usecase.stockimport.impl;

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
import com.undercontroll.domain.model.stockimport.InvoiceFileType;
import com.undercontroll.domain.model.stockimport.InvoiceUpload;
import com.undercontroll.domain.model.stockimport.StockImport;
import com.undercontroll.domain.support.Texts;
import com.undercontroll.domain.usecase.stockimport.CreateStockImportPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.OptionalInt;

@Slf4j
@Service
public class CreateStockImportImpl implements CreateStockImportPort {

    public static final long MAX_BYTES = 10L * 1024 * 1024;
    static final int MAX_PDF_PAGES = 5;

    private final StockImportGateway stockImportGateway;
    private final ObjectProvider<InvoiceExtractionGateway> invoiceExtractionGateway;
    private final PdfInspectorGateway pdfInspectorGateway;
    private final StockImportDispatcher stockImportDispatcher;
    private final CurrentUserIdPort currentUserIdPort;
    private final Clock clock;

    public CreateStockImportImpl(
            StockImportGateway stockImportGateway,
            ObjectProvider<InvoiceExtractionGateway> invoiceExtractionGateway,
            PdfInspectorGateway pdfInspectorGateway,
            StockImportDispatcher stockImportDispatcher,
            CurrentUserIdPort currentUserIdPort,
            Clock clock
    ) {
        this.stockImportGateway = stockImportGateway;
        this.invoiceExtractionGateway = invoiceExtractionGateway;
        this.pdfInspectorGateway = pdfInspectorGateway;
        this.stockImportDispatcher = stockImportDispatcher;
        this.currentUserIdPort = currentUserIdPort;
        this.clock = clock;
    }

    @Override
    public StockImport execute(InvoiceUpload upload) {
        InvoiceFileType type = validate(upload);
        if (type.sourceType() != StockImportSourceType.NFE_XML && invoiceExtractionGateway.getIfAvailable() == null) {
            throw new StockImportUnavailableException();
        }
        Instant now = clock.instant();
        StockImport created = stockImportGateway.create(StockImport.builder()
                .createdBy(currentUserIdPort.require())
                .status(StockImportStatus.PROCESSING)
                .stage(StockImportStage.RECEIVED)
                .sourceType(type.sourceType())
                .fileName(Texts.clip(Texts.blankToNull(upload.fileName()), 255))
                .mimeType(type.mimeType())
                .fileSha256(sha256(upload.content()))
                .createdAt(now)
                .updatedAt(now)
                .build(), upload.content());
        log.info("Stock import created id={} sourceType={} byteSize={}", created.id(), created.sourceType(), upload.size());
        stockImportDispatcher.dispatch();
        return created;
    }

    InvoiceFileType validate(InvoiceUpload upload) {
        if (upload == null || upload.content() == null) {
            throw new InvalidStockImportException("File is required");
        }
        if (upload.size() == 0) {
            throw new InvalidStockImportException("File is empty");
        }
        if (upload.size() > MAX_BYTES) {
            throw new InvalidStockImportException("File exceeds 10MB");
        }
        String declared = mediaType(upload.contentType());
        if ("image/heic".equals(declared) || "image/heif".equals(declared)) {
            throw new InvalidStockImportException("HEIC is not supported; send the photo as JPEG or PNG");
        }
        InvoiceFileType detected = InvoiceFileType.detect(upload.content())
                .orElseThrow(() -> new InvalidStockImportException(
                        "Unsupported or unrecognized file; use JPEG, PNG, WEBP, PDF or NF-e XML"));
        if (declared != null) {
            InvoiceFileType declaredType = InvoiceFileType.fromMimeType(declared)
                    .orElseThrow(() -> new InvalidStockImportException("Unsupported file type " + declared));
            if (declaredType != detected) {
                throw new InvalidStockImportException("Declared type " + declared
                        + " does not match the file content (" + detected.mimeType() + ")");
            }
        }
        if (detected == InvoiceFileType.PDF) {
            OptionalInt pages = pdfInspectorGateway.pageCount(upload.content());
            if (pages.isEmpty()) {
                throw new InvalidStockImportException("PDF could not be read (corrupted or password protected)");
            }
            if (pages.getAsInt() > MAX_PDF_PAGES) {
                throw new InvalidStockImportException("PDF has more than " + MAX_PDF_PAGES + " pages");
            }
        }
        return detected;
    }

    private static String mediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        String type = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return type.isEmpty() || "application/octet-stream".equals(type) ? null : type;
    }

    static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
