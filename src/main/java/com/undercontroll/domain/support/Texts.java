package com.undercontroll.domain.support;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Texts {

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9\\s]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private Texts() {
    }

    public static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    public static String stripAccents(String value) {
        if (value == null) {
            return null;
        }
        return COMBINING_MARKS.matcher(Normalizer.normalize(value, Normalizer.Form.NFD)).replaceAll("");
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String folded = NON_ALPHANUMERIC.matcher(stripAccents(value.toLowerCase(Locale.ROOT))).replaceAll(" ").strip();
        return WHITESPACE.matcher(folded).replaceAll(" ");
    }

    public static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }

    public static String clip(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max).strip();
    }
}
