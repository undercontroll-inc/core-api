package com.undercontroll.domain.storage;

import com.undercontroll.domain.model.stockimport.InvoiceFileType;

import java.time.Duration;
import java.util.EnumSet;

import static com.undercontroll.domain.model.stockimport.InvoiceFileType.JPEG;
import static com.undercontroll.domain.model.stockimport.InvoiceFileType.PDF;
import static com.undercontroll.domain.model.stockimport.InvoiceFileType.PNG;
import static com.undercontroll.domain.model.stockimport.InvoiceFileType.WEBP;
import static com.undercontroll.domain.model.stockimport.InvoiceFileType.XML;

public enum StorageArea {

    ANNOUNCEMENT_IMAGES(new StoragePolicy(StorageBucket.UPLOADS, "announcements", EnumSet.of(JPEG, PNG, WEBP),
            5L * 1024 * 1024, Duration.ofMinutes(5), Duration.ofMinutes(60), false)),
    ORDER_PHOTOS(new StoragePolicy(StorageBucket.UPLOADS, "order-photos", EnumSet.of(JPEG, PNG, WEBP),
            10L * 1024 * 1024, Duration.ofMinutes(5), Duration.ofMinutes(15), false)),
    LABEL_SCANS(new StoragePolicy(StorageBucket.UPLOADS, "label-scans", EnumSet.of(JPEG, PNG, WEBP),
            10L * 1024 * 1024, Duration.ofMinutes(5), Duration.ZERO, false)),
    STOCK_IMPORTS(new StoragePolicy(StorageBucket.UPLOADS, "stock-imports", EnumSet.of(JPEG, PNG, WEBP, PDF, XML),
            10L * 1024 * 1024, Duration.ofMinutes(5), Duration.ZERO, false)),
    ORDER_EXPORTS(new StoragePolicy(StorageBucket.EXPORTS, "order-exports", EnumSet.of(PDF),
            20L * 1024 * 1024, Duration.ZERO, Duration.ofMinutes(5), true));

    private final StoragePolicy policy;

    StorageArea(StoragePolicy policy) {
        this.policy = policy;
    }

    public StoragePolicy policy() {
        return policy;
    }

    public boolean clientReadable() {
        return !policy.readTtl().isZero();
    }

    public boolean clientWritable() {
        return !policy.uploadTtl().isZero();
    }

    public boolean owns(String key) {
        return key != null && key.startsWith(policy.prefix() + "/") && !key.contains("..");
    }

    public static String extension(InvoiceFileType type) {
        return switch (type) {
            case JPEG -> "jpg";
            case PNG -> "png";
            case WEBP -> "webp";
            case PDF -> "pdf";
            case XML -> "xml";
        };
    }
}
