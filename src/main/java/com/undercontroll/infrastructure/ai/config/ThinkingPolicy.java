package com.undercontroll.infrastructure.ai.config;

import java.util.Arrays;
import java.util.Locale;

public enum ThinkingPolicy {

    DEFAULT,
    OFF,
    MINIMAL,
    LOW,
    HIGH;

    public static ThinkingPolicy parse(String feature, String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(policy -> policy.name().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Invalid undercontroll.ai." + feature + ".thinking='" + value
                                + "'. Use one of: default, off, minimal, low, high"));
    }

    public int budgetTokens() {
        return switch (this) {
            case OFF -> 0;
            case HIGH -> 8192;
            default -> 1024;
        };
    }

    public String label() {
        return name().toLowerCase(Locale.ROOT);
    }
}
