package com.undercontroll.domain.model.chat;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class AnaWebSearchNeed {

    private static final Pattern NEED = Pattern.compile(
            "\\bmanual(?:is)?\\b|\\brecall\\b|\\bdica\\b|\\binternet\\b|\\bweb\\b|\\bcomo\\s+limpar\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private AnaWebSearchNeed() {
    }

    public static boolean matches(String text) {
        return text != null && NEED.matcher(text.toLowerCase(Locale.ROOT)).find();
    }

    public static boolean matchesConversation(String current, List<String> history) {
        if (matches(current)) {
            return true;
        }
        if (history == null || history.isEmpty()) {
            return false;
        }
        return history.stream().anyMatch(AnaWebSearchNeed::matches);
    }
}
