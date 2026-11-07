package com.undercontroll.domain.model.chat;

import com.undercontroll.domain.exception.InvalidChatHistoryRequestException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;

public final class ChatCursor {

    private static final String CONVERSATIONS = "c";
    private static final String MESSAGES = "m";
    private static final String SEPARATOR = "|";

    private ChatCursor() {
    }

    public record ConversationPosition(Instant lastMessageAt, long id) {
    }

    public static String conversations(Conversation last) {
        return encode(CONVERSATIONS + SEPARATOR + last.lastMessageAt() + SEPARATOR + last.id());
    }

    public static ConversationPosition parseConversations(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        String[] parts = decode(cursor).split("\\|", -1);
        if (parts.length != 3 || !CONVERSATIONS.equals(parts[0])) {
            throw invalid();
        }
        try {
            return new ConversationPosition(Instant.parse(parts[1]), Long.parseLong(parts[2]));
        } catch (DateTimeParseException | NumberFormatException ex) {
            throw invalid();
        }
    }

    public static String messages(int beforeSeq) {
        return encode(MESSAGES + SEPARATOR + beforeSeq);
    }

    public static Integer parseMessages(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        String[] parts = decode(cursor).split("\\|", -1);
        if (parts.length != 2 || !MESSAGES.equals(parts[0])) {
            throw invalid();
        }
        try {
            int seq = Integer.parseInt(parts[1]);
            if (seq < 1) {
                throw invalid();
            }
            return seq;
        } catch (NumberFormatException ex) {
            throw invalid();
        }
    }

    private static String encode(String raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String cursor) {
        try {
            return new String(Base64.getUrlDecoder().decode(cursor.strip()), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw invalid();
        }
    }

    private static InvalidChatHistoryRequestException invalid() {
        return new InvalidChatHistoryRequestException("cursor", "cursor is invalid");
    }
}
