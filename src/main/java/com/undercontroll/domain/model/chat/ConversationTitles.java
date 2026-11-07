package com.undercontroll.domain.model.chat;

public final class ConversationTitles {

    public static final int MAX_TITLE = 120;
    static final int AUTO_TITLE = 60;
    static final int MIN_WORD_CUT = 20;
    static final int MAX_PREVIEW = 160;
    static final String FALLBACK = "Nova conversa";
    static final String ELLIPSIS = "…";

    private ConversationTitles() {
    }

    public static String title(String firstMessage) {
        if (firstMessage == null) {
            return FALLBACK;
        }
        String line = firstMessage.strip().lines()
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .findFirst()
                .orElse("");
        String collapsed = collapse(line);
        if (collapsed.isEmpty()) {
            return FALLBACK;
        }
        if (collapsed.length() <= AUTO_TITLE) {
            return collapsed;
        }
        int cut = collapsed.lastIndexOf(' ', AUTO_TITLE);
        String head = cut >= MIN_WORD_CUT ? collapsed.substring(0, cut) : collapsed.substring(0, AUTO_TITLE);
        return head.strip() + ELLIPSIS;
    }

    public static String preview(String content) {
        String collapsed = collapse(content);
        if (collapsed.length() <= MAX_PREVIEW) {
            return collapsed;
        }
        return collapsed.substring(0, MAX_PREVIEW - 1).strip() + ELLIPSIS;
    }

    private static String collapse(String value) {
        return value == null ? "" : value.strip().replaceAll("\\s+", " ");
    }
}
