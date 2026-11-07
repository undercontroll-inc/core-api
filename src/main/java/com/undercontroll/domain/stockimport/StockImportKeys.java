package com.undercontroll.domain.stockimport;

import com.undercontroll.domain.support.Texts;

import java.util.Optional;

public final class StockImportKeys {

    static final int MAX_KEY_LENGTH = 255;

    private StockImportKeys() {
    }

    public static Optional<String> supplierKey(String cnpj, String supplierName) {
        String digits = digits(cnpj);
        if (digits != null && digits.length() >= 11) {
            return Optional.of("cnpj:" + digits);
        }
        String name = Texts.normalize(supplierName);
        return name.isEmpty() ? Optional.empty() : Optional.of(Texts.clip("name:" + name, MAX_KEY_LENGTH));
    }

    public static Optional<String> productKey(String supplierCode, String description) {
        String code = Texts.blankToNull(supplierCode);
        if (code != null) {
            return Optional.of(Texts.clip("code:" + code.toLowerCase(java.util.Locale.ROOT), MAX_KEY_LENGTH));
        }
        String normalized = Texts.normalize(description);
        return normalized.isEmpty() ? Optional.empty() : Optional.of(Texts.clip("desc:" + normalized, MAX_KEY_LENGTH));
    }

    public static String digits(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }
}
