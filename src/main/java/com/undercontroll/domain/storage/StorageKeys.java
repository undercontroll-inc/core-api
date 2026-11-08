package com.undercontroll.domain.storage;

import com.undercontroll.domain.model.stockimport.InvoiceFileType;

import java.util.UUID;
import java.util.regex.Pattern;

public final class StorageKeys {

    private static final Pattern SEGMENT = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    private StorageKeys() {
    }

    public static String generate(StorageArea area, String owner, InvoiceFileType type) {
        if (owner == null || !SEGMENT.matcher(owner).matches()) {
            throw new IllegalArgumentException("Invalid storage key segment");
        }
        return area.policy().prefix() + "/" + owner + "/" + UUID.randomUUID() + "." + StorageArea.extension(type);
    }
}
