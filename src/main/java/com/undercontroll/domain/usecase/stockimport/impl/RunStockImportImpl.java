package com.undercontroll.domain.usecase.stockimport.impl;

import com.undercontroll.domain.enums.StockImportFailureReason;
import com.undercontroll.domain.enums.StockImportItemAction;
import com.undercontroll.domain.enums.StockImportSourceType;
import com.undercontroll.domain.enums.StockImportStage;
import com.undercontroll.domain.enums.StockImportStatus;
import com.undercontroll.domain.exception.InvalidNfeXmlException;
import com.undercontroll.domain.exception.InvoiceExtractionException;
import com.undercontroll.domain.gateway.InvoiceExtractionGateway;
import com.undercontroll.domain.exception.InvalidUploadException;
import com.undercontroll.domain.gateway.NfeXmlGateway;
import com.undercontroll.domain.gateway.ObjectStorageGateway;
import com.undercontroll.domain.storage.StorageArea;
import com.undercontroll.domain.gateway.StockImportGateway;
import com.undercontroll.domain.insight.InsightRetryPolicy;
import com.undercontroll.domain.model.insight.LlmFailure;
import com.undercontroll.domain.model.stockimport.ComponentMatch;
import com.undercontroll.domain.model.stockimport.InvoiceExtraction;
import com.undercontroll.domain.model.stockimport.InvoiceFile;
import com.undercontroll.domain.model.stockimport.StockImport;
import com.undercontroll.domain.model.stockimport.StockImportItem;
import com.undercontroll.domain.stockimport.ComponentMatcher;
import com.undercontroll.domain.stockimport.StockImportKeys;
import com.undercontroll.domain.stockimport.StockImportWarnings;
import com.undercontroll.domain.support.Deadline;
import com.undercontroll.domain.support.Durations;
import com.undercontroll.domain.support.Sleeper;
import com.undercontroll.domain.support.Texts;
import com.undercontroll.domain.usecase.stockimport.RunStockImportPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

@Slf4j
@Service
public class RunStockImportImpl implements RunStockImportPort {

    static final int MAX_ATTEMPTS = 3;

    private final StockImportGateway stockImportGateway;
    private final ObjectProvider<InvoiceExtractionGateway> invoiceExtractionGateway;
    private final NfeXmlGateway nfeXmlGateway;
    private final ObjectStorageGateway storage;
    private final ComponentMatcher componentMatcher;
    private final Sleeper sleeper;
    private final RandomGenerator random;
    private final Clock clock;
    private final Duration deadline;
    private final InsightRetryPolicy retryPolicy = InsightRetryPolicy.DEFAULT;

    public RunStockImportImpl(
            StockImportGateway stockImportGateway,
            ObjectProvider<InvoiceExtractionGateway> invoiceExtractionGateway,
            NfeXmlGateway nfeXmlGateway,
            ObjectStorageGateway storage,
            ComponentMatcher componentMatcher,
            Sleeper sleeper,
            RandomGenerator random,
            Clock clock,
            @Value("${undercontroll.ai.stock-import.job-deadline:90s}") Duration deadline
    ) {
        this.stockImportGateway = stockImportGateway;
        this.invoiceExtractionGateway = invoiceExtractionGateway;
        this.nfeXmlGateway = nfeXmlGateway;
        this.storage = storage;
        this.componentMatcher = componentMatcher;
        this.sleeper = sleeper;
        this.random = random;
        this.clock = clock;
        this.deadline = deadline;
    }

    @Override
    public void execute(StockImport leased, String owner) {
        long started = System.nanoTime();
        Long importId = leased.id();
        try {
            if (leased.attemptCount() > MAX_ATTEMPTS) {
                throw new Failure(StockImportFailureReason.TIMEOUT, "Extraction did not finish after " + MAX_ATTEMPTS + " attempts");
            }
            byte[] content = content(leased);
            stockImportGateway.updateStage(importId, owner, StockImportStage.EXTRACTING, clock.instant());
            InvoiceExtraction extraction = extract(leased, content);
            if (!extraction.invoice()) {
                throw new Failure(StockImportFailureReason.NOT_AN_INVOICE, "The file does not look like an invoice");
            }
            if (extraction.items().isEmpty()) {
                throw new Failure(StockImportFailureReason.UNREADABLE, "No line items could be read from the invoice");
            }
            stockImportGateway.updateStage(importId, owner, StockImportStage.MATCHING, clock.instant());
            StockImport ready = ready(leased, extraction);
            if (stockImportGateway.complete(ready, owner, clock.instant())) {
                log.info("Stock import ready id={} sourceType={} items={} warnings={} durationMs={}",
                        importId, leased.sourceType(), ready.itemCount(), ready.warnings(), Durations.millisSince(started));
            } else {
                log.info("Stock import id={} finished after its lease was lost or it was canceled", importId);
            }
        } catch (Failure failure) {
            fail(importId, owner, failure.reason, failure.getMessage(), started);
        } catch (RuntimeException ex) {
            log.error("Stock import id={} failed unexpectedly", importId, ex);
            fail(importId, owner, StockImportFailureReason.UNREADABLE, "Unexpected error while reading the invoice", started);
        }
    }

    private byte[] content(StockImport leased) {
        if (leased.storageKey() == null) {
            return stockImportGateway.findLegacyContent(leased.id())
                    .orElseThrow(() -> new Failure(StockImportFailureReason.UNREADABLE, "The uploaded file is no longer available"));
        }
        try {
            return storage.read(StorageArea.STOCK_IMPORTS, leased.storageKey(), StorageArea.STOCK_IMPORTS.policy().maxBytes());
        } catch (InvalidUploadException ex) {
            throw new Failure(StockImportFailureReason.UNREADABLE, "The uploaded file is no longer available");
        }
    }

    private void fail(Long importId, String owner, StockImportFailureReason reason, String message, long started) {
        stockImportGateway.fail(importId, owner, reason, message, clock.instant());
        log.warn("Stock import failed id={} reason={} message={} durationMs={}",
                importId, reason, message, Durations.millisSince(started));
    }

    private InvoiceExtraction extract(StockImport leased, byte[] content) {
        if (leased.sourceType() == StockImportSourceType.NFE_XML) {
            try {
                return nfeXmlGateway.parse(content);
            } catch (InvalidNfeXmlException ex) {
                throw new Failure(StockImportFailureReason.INVALID_XML, ex.getMessage());
            }
        }
        InvoiceExtractionGateway gateway = invoiceExtractionGateway.getIfAvailable();
        if (gateway == null) {
            throw new Failure(StockImportFailureReason.AI_UNAVAILABLE, "Invoice extraction is currently unavailable");
        }
        InvoiceFile file = new InvoiceFile(content, leased.mimeType(), leased.sourceType());
        Deadline budget = Deadline.after(deadline);
        int retry = 0;
        while (true) {
            try {
                return gateway.extract(file);
            } catch (InvoiceExtractionException ex) {
                LlmFailure failure = ex.failure();
                if (failure.retryable() && retry < retryPolicy.maxRetries()) {
                    Duration wait = retryPolicy.backoff(++retry, failure, random.nextDouble());
                    if (budget.remaining().compareTo(wait) > 0) {
                        log.info("Stock import id={} retry={} kind={} waitMs={}", leased.id(), retry, failure.kind(), wait.toMillis());
                        sleep(wait);
                        continue;
                    }
                }
                throw new Failure(reason(failure), message(failure));
            }
        }
    }

    private void sleep(Duration wait) {
        try {
            sleeper.sleep(wait);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new Failure(StockImportFailureReason.AI_UNAVAILABLE, "Invoice extraction was interrupted");
        }
    }

    StockImport ready(StockImport leased, InvoiceExtraction extraction) {
        String supplierName = Texts.clip(Texts.blankToNull(extraction.supplierName()), 255);
        String supplierCnpj = StockImportKeys.digits(extraction.supplierCnpj());
        String supplierKey = StockImportKeys.supplierKey(supplierCnpj, supplierName).orElse(null);
        List<StockImportItem> items = new ArrayList<>();
        int line = 1;
        for (InvoiceExtraction.ExtractedItem extracted : extraction.items()) {
            String description = Texts.clip(Texts.blankToNull(extracted.description()), 255);
            if (description == null) {
                continue;
            }
            boolean quantityKnown = extracted.quantity() != null && extracted.quantity().signum() > 0;
            double confidence = Math.clamp(extracted.confidence(), 0.0, 1.0);
            String supplierCode = Texts.clip(Texts.blankToNull(extracted.supplierCode()), 60);
            ComponentMatch match = componentMatcher.match(supplierKey, supplierCode, description);
            items.add(StockImportWarnings.withWarnings(StockImportItem.builder()
                    .lineNumber(line++)
                    .supplierCode(supplierCode)
                    .description(description)
                    .quantity(quantityKnown ? extracted.quantity() : BigDecimal.ONE)
                    .unit(Texts.clip(Texts.blankToNull(extracted.unit()), 20))
                    .unitPrice(nonNegative(extracted.unitPrice()))
                    .total(nonNegative(extracted.total()))
                    .confidence(quantityKnown ? confidence : Math.min(confidence, 0.5))
                    .matchType(match.type())
                    .matchedComponentId(match.componentId())
                    .matchConfidence(match.confidence())
                    .candidates(match.candidates())
                    .action(match.componentId() != null ? StockImportItemAction.ADD_TO_EXISTING : StockImportItemAction.CREATE_NEW)
                    .componentId(match.componentId())
                    .build()));
        }
        if (items.isEmpty()) {
            throw new Failure(StockImportFailureReason.UNREADABLE, "No line items could be read from the invoice");
        }
        String accessKey = StockImportKeys.digits(extraction.accessKey());
        if (accessKey != null && accessKey.length() != 44) {
            accessKey = null;
        }
        Optional<Long> duplicate = stockImportGateway.findConfirmedDuplicate(leased.id(), accessKey, leased.fileSha256());
        StockImport ready = leased.toBuilder()
                .status(StockImportStatus.READY)
                .stage(null)
                .supplierName(supplierName)
                .supplierCnpj(supplierCnpj)
                .invoiceNumber(Texts.clip(Texts.blankToNull(extraction.number()), 60))
                .invoiceSeries(Texts.clip(Texts.blankToNull(extraction.series()), 20))
                .issuedAt(extraction.issuedAt())
                .accessKey(accessKey)
                .total(nonNegative(extraction.total()))
                .duplicateOfImportId(duplicate.orElse(null))
                .failureReason(null)
                .failureMessage(null)
                .items(items)
                .itemCount(items.size())
                .build();
        return ready.toBuilder().warnings(StockImportWarnings.importWarnings(ready)).build();
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        return value == null || value.signum() < 0 ? null : value;
    }

    private static StockImportFailureReason reason(LlmFailure failure) {
        return switch (failure.kind()) {
            case TIMEOUT -> StockImportFailureReason.TIMEOUT;
            case INVALID_OUTPUT -> StockImportFailureReason.UNREADABLE;
            default -> StockImportFailureReason.AI_UNAVAILABLE;
        };
    }

    private static String message(LlmFailure failure) {
        return switch (failure.kind()) {
            case TIMEOUT -> "Invoice extraction timed out";
            case INVALID_OUTPUT -> "The invoice could not be read; try a sharper photo";
            default -> "Invoice extraction is temporarily unavailable";
        };
    }

    static final class Failure extends RuntimeException {

        private final StockImportFailureReason reason;

        Failure(StockImportFailureReason reason, String message) {
            super(message);
            this.reason = reason;
        }
    }
}
