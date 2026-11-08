package com.undercontroll.domain.storage;

import com.undercontroll.domain.model.stockimport.InvoiceFileType;

import java.time.Duration;
import java.util.Set;

public record StoragePolicy(
        StorageBucket bucket,
        String prefix,
        Set<InvoiceFileType> allowedTypes,
        long maxBytes,
        Duration uploadTtl,
        Duration readTtl,
        boolean attachment
) {

    public boolean allows(InvoiceFileType type) {
        return allowedTypes.contains(type);
    }
}
