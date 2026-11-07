package com.undercontroll.domain.model.chat;

import com.undercontroll.domain.exception.InvalidChatHistoryRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatHistoryModelTest {

    @Test
    @DisplayName("title uses the first non-blank line, collapses spaces and cuts long text at a word")
    void titles() {
        assertEquals("Quantos motores temos?", ConversationTitles.title("\n  Quantos   motores temos?\nE fusíveis?"));
        assertEquals("Nova conversa", ConversationTitles.title("   "));
        String title = ConversationTitles.title("Preciso de um orçamento completo para o conserto do liquidificador da cliente Maria");
        assertTrue(title.length() <= 61);
        assertTrue(title.endsWith("…"));
        assertEquals("Preciso de um orçamento completo para o conserto do…", title);
    }

    @Test
    @DisplayName("preview keeps up to 160 characters on one line")
    void preview() {
        assertEquals("a b", ConversationTitles.preview("a\n\nb"));
        assertEquals(160, ConversationTitles.preview("x".repeat(500)).length());
    }

    @Test
    @DisplayName("cursors round-trip and reject tampering")
    void cursors() {
        Instant at = Instant.parse("2026-11-07T12:00:00.123456Z");
        Conversation last = new Conversation(9L, 1, "a", "t", Conversation.TitleSource.AUTO, "p", 1, 1, at, at, at);
        ChatCursor.ConversationPosition position = ChatCursor.parseConversations(ChatCursor.conversations(last));
        assertEquals(at, position.lastMessageAt());
        assertEquals(9L, position.id());
        assertEquals(41, ChatCursor.parseMessages(ChatCursor.messages(41)));
        assertNull(ChatCursor.parseMessages(null));
        assertNull(ChatCursor.parseConversations(" "));

        assertThrows(InvalidChatHistoryRequestException.class, () -> ChatCursor.parseMessages("%%%"));
        assertThrows(InvalidChatHistoryRequestException.class, () -> ChatCursor.parseMessages(ChatCursor.conversations(last)));
        assertThrows(InvalidChatHistoryRequestException.class, () -> ChatCursor.parseConversations(ChatCursor.messages(3)));
        assertThrows(InvalidChatHistoryRequestException.class, () -> ChatCursor.parseMessages(ChatCursor.messages(0)));
    }
}
