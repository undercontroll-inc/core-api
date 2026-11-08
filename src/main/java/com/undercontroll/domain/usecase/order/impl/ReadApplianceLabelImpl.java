package com.undercontroll.domain.usecase.order.impl;

import com.undercontroll.domain.exception.InvalidLabelReadingException;
import com.undercontroll.domain.exception.InvalidUploadException;
import com.undercontroll.domain.exception.LabelReadingUnavailableException;
import com.undercontroll.domain.gateway.LabelExtractionGateway;
import com.undercontroll.domain.gateway.ObjectStorageGateway;
import com.undercontroll.domain.model.LabelReading;
import com.undercontroll.domain.model.stockimport.InvoiceFile;
import com.undercontroll.domain.model.stockimport.InvoiceFileType;
import com.undercontroll.domain.storage.StorageArea;
import com.undercontroll.domain.storage.UploadIntent;
import com.undercontroll.domain.storage.UploadTicket;
import com.undercontroll.domain.usecase.order.ReadApplianceLabelPort;
import com.undercontroll.domain.usecase.order.RequestLabelUploadPort;
import com.undercontroll.domain.usecase.storage.StorageUploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
public class ReadApplianceLabelImpl implements ReadApplianceLabelPort, RequestLabelUploadPort {

    private final ObjectProvider<LabelExtractionGateway> gateway;
    private final StorageUploadService uploads;
    private final ObjectStorageGateway storage;

    public ReadApplianceLabelImpl(
            ObjectProvider<LabelExtractionGateway> gateway,
            StorageUploadService uploads,
            ObjectStorageGateway storage
    ) {
        this.gateway = gateway;
        this.uploads = uploads;
        this.storage = storage;
    }

    @Override
    public UploadTicket execute(String contentType, Long sizeBytes) {
        if (gateway.getIfAvailable() == null) {
            throw new LabelReadingUnavailableException();
        }
        try {
            return uploads.request(StorageArea.LABEL_SCANS, contentType, sizeBytes, null, null, null);
        } catch (InvalidUploadException ex) {
            throw new InvalidLabelReadingException(ex.getMessage());
        }
    }

    @Override
    public LabelReading execute(UUID uploadId) {
        LabelExtractionGateway extractor = gateway.getIfAvailable();
        if (extractor == null) {
            throw new LabelReadingUnavailableException();
        }
        if (uploadId == null) {
            throw new InvalidLabelReadingException("Informe o uploadId da foto da etiqueta");
        }
        UploadIntent upload;
        byte[] content;
        try {
            upload = uploads.complete(uploadId, StorageArea.LABEL_SCANS, null);
            content = storage.read(StorageArea.LABEL_SCANS, upload.objectKey(), StorageArea.LABEL_SCANS.policy().maxBytes());
        } catch (InvalidUploadException ex) {
            throw new InvalidLabelReadingException(ex.getMessage());
        }
        InvoiceFileType type = InvoiceFileType.fromMimeType(upload.contentType()).orElseThrow();
        try {
            return normalize(extractor.extract(new InvoiceFile(content, type.mimeType(), type.sourceType())));
        } catch (InvalidLabelReadingException | LabelReadingUnavailableException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Label reading failed cause={}", ex.toString());
            throw new LabelReadingUnavailableException(ex);
        }
    }

    static LabelReading normalize(LabelReading reading) {
        if (!reading.isLabel()) {
            return new LabelReading(false, null, null, null, null, null, Math.clamp(reading.confidence(), 0.0, 1.0));
        }
        String voltage = reading.voltage() == null ? null : reading.voltage().replace(" ", "").toUpperCase(Locale.ROOT);
        if ("110V".equals(voltage) || "127/110V".equals(voltage)) {
            voltage = "127V";
        }
        return new LabelReading(
                true,
                clean(reading.type()),
                clean(reading.brand()),
                clean(reading.model()),
                clean(reading.serial()),
                LabelReading.VOLTAGES.contains(voltage) ? voltage : null,
                Math.clamp(reading.confidence(), 0.0, 1.0)
        );
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.length() > 120 ? trimmed.substring(0, 120) : trimmed;
    }
}
