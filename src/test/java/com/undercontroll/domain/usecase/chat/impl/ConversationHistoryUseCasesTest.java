package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.domain.exception.ConversationMemoryUnavailableException;
import com.undercontroll.domain.exception.ConversationNotFoundException;
import com.undercontroll.domain.exception.InvalidChatHistoryRequestException;
import com.undercontroll.domain.gateway.AnaConversationMemory;
import com.undercontroll.domain.gateway.ConversationGateway;
import com.undercontroll.domain.gateway.ConversationListVersion;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.model.chat.ChatCursor;
import com.undercontroll.domain.model.chat.Conversation;
import com.undercontroll.domain.model.chat.ConversationKey;
import com.undercontroll.domain.model.chat.ConversationPage;
import com.undercontroll.domain.model.chat.MessagePage;
import com.undercontroll.domain.usecase.chat.ListConversationMessagesPort;
import com.undercontroll.domain.usecase.chat.ListConversationsPort;
import com.undercontroll.domain.usecase.chat.RenameConversationPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConversationHistoryUseCasesTest {

    private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final Conversation CONVERSATION = new Conversation(
            9L, 7, "kitchen", "Oi", Conversation.TitleSource.AUTO, "Oi", 4, 4, NOW, NOW, NOW);

    private final CurrentUserIdPort user = () -> 7;
    private ConversationGateway conversations;
    private ConversationListVersion listVersion;
    private ConversationReadQuery readQuery;
    private AnaConversationMemory memory;

    @BeforeEach
    void setUp() {
        conversations = mock(ConversationGateway.class);
        listVersion = mock(ConversationListVersion.class);
        readQuery = mock(ConversationReadQuery.class);
        memory = mock(AnaConversationMemory.class);
    }

    @Test
    @DisplayName("list reads through the versioned query and validates limit and cursor")
    void list() {
        ListConversationsImpl list = new ListConversationsImpl(user, listVersion, readQuery);
        ConversationPage page = new ConversationPage(new ArrayList<>(), null);
        when(listVersion.current(7)).thenReturn(3L);
        when(readQuery.conversations(7, 3L, null, 20)).thenReturn(page);

        assertSame(page, list.execute(new ListConversationsPort.Input(null, null)));
        assertThrows(InvalidChatHistoryRequestException.class, () -> list.execute(new ListConversationsPort.Input(null, 51)));
        assertThrows(InvalidChatHistoryRequestException.class, () -> list.execute(new ListConversationsPort.Input("nope", 10)));
    }

    @Test
    @DisplayName("messages check ownership on every read and expose a version for the ETag")
    void messages() {
        ListConversationMessagesImpl messages = new ListConversationMessagesImpl(user, conversations, readQuery);
        MessagePage page = new MessagePage(new ArrayList<>(), null);
        when(conversations.findActive(7, "kitchen")).thenReturn(Optional.of(CONVERSATION));
        when(readQuery.messages(9L, 4, null, 50)).thenReturn(page);
        when(readQuery.messages(9L, 4, 3, 10)).thenReturn(page);

        ListConversationMessagesPort.Output latest = messages.execute(new ListConversationMessagesPort.Input("kitchen", null, null));
        ListConversationMessagesPort.Output older = messages.execute(
                new ListConversationMessagesPort.Input("kitchen", ChatCursor.messages(3), 10));

        assertSame(page, latest.page());
        assertEquals("9-4-latest-50", latest.version());
        assertEquals("9-4-3-10", older.version());
        assertThrows(ConversationNotFoundException.class,
                () -> messages.execute(new ListConversationMessagesPort.Input("other", null, null)));
        assertThrows(ConversationNotFoundException.class,
                () -> messages.execute(new ListConversationMessagesPort.Input("bad id!", null, null)));
    }

    @Test
    @DisplayName("rename trims, validates and bumps the list version")
    void rename() {
        RenameConversationImpl rename = new RenameConversationImpl(user, conversations, listVersion, CLOCK);
        when(conversations.rename(7, "kitchen", "Estoque de motores", NOW)).thenReturn(Optional.of(CONVERSATION));

        assertSame(CONVERSATION, rename.execute(new RenameConversationPort.Input("kitchen", "  Estoque   de motores ")));
        verify(listVersion).bump(7);
        assertThrows(InvalidChatHistoryRequestException.class,
                () -> rename.execute(new RenameConversationPort.Input("kitchen", " ")));
        assertThrows(InvalidChatHistoryRequestException.class,
                () -> rename.execute(new RenameConversationPort.Input("kitchen", "x".repeat(121))));
        assertThrows(ConversationNotFoundException.class,
                () -> rename.execute(new RenameConversationPort.Input("ghost", "x")));
    }

    @Test
    @DisplayName("delete is idempotent, clears Redis memory and survives a Redis outage")
    void delete() {
        DeleteConversationImpl delete = new DeleteConversationImpl(user, conversations, listVersion, memory, CLOCK);
        ConversationKey key = new ConversationKey(7, "kitchen");
        when(conversations.softDelete(7, "kitchen", NOW)).thenReturn(true);
        doThrow(new ConversationMemoryUnavailableException(new IllegalStateException("redis"))).when(memory).clear(key);

        delete.execute("kitchen");

        verify(listVersion).bump(7);
        verify(memory).clear(key);

    }

    @Test
    @DisplayName("deleting a missing or already deleted conversation does nothing")
    void deleteMissing() {
        DeleteConversationImpl delete = new DeleteConversationImpl(user, conversations, listVersion, memory, CLOCK);

        delete.execute("kitchen");

        verify(listVersion, never()).bump(any());
        verifyNoInteractions(memory);
    }

    @Test
    @DisplayName("get returns only active conversations of the current user")
    void get() {
        GetConversationImpl get = new GetConversationImpl(user, conversations);
        when(conversations.findActive(7, "kitchen")).thenReturn(Optional.of(CONVERSATION));

        assertSame(CONVERSATION, get.execute("kitchen"));
        assertThrows(ConversationNotFoundException.class, () -> get.execute("gone"));
        verifyNoInteractions(readQuery);
        verify(conversations, never()).list(any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }
}
