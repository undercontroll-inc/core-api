package com.undercontroll.application.controller;

import com.undercontroll.application.controller.impl.ChatConversationController;
import com.undercontroll.application.mapper.ChatConversationDtoMapperImpl;
import com.undercontroll.domain.exception.ConversationNotFoundException;
import com.undercontroll.domain.exception.InvalidChatHistoryRequestException;
import com.undercontroll.domain.gateway.TokenGateway;
import com.undercontroll.domain.model.chat.AnaReply;
import com.undercontroll.domain.model.chat.ChatMessage;
import com.undercontroll.domain.model.chat.Conversation;
import com.undercontroll.domain.model.chat.ConversationPage;
import com.undercontroll.domain.model.chat.MessagePage;
import com.undercontroll.domain.model.chat.StoredChatMessage;
import com.undercontroll.domain.usecase.chat.DeleteConversationPort;
import com.undercontroll.domain.usecase.chat.GetConversationPort;
import com.undercontroll.domain.usecase.chat.ListConversationMessagesPort;
import com.undercontroll.domain.usecase.chat.ListConversationsPort;
import com.undercontroll.domain.usecase.chat.RenameConversationPort;
import com.undercontroll.infrastructure.config.SecurityConfig;
import com.undercontroll.infrastructure.handler.ChatExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({SecurityConfig.class, ChatExceptionHandler.class, ChatConversationDtoMapperImpl.class})
@AutoConfigureMockMvc
@WebMvcTest(ChatConversationController.class)
class ChatConversationControllerTest {

    private static final String BASE = "/v1/api/chats/conversations";
    private static final Instant NOW = Instant.parse("2026-11-07T12:00:00Z");
    private static final Conversation CONVERSATION = new Conversation(
            9L, 7, "kitchen", "Motores", Conversation.TitleSource.AUTO, "Temos 2.", 2, 2, NOW, NOW, NOW);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListConversationsPort listConversationsPort;
    @MockitoBean
    private GetConversationPort getConversationPort;
    @MockitoBean
    private ListConversationMessagesPort listConversationMessagesPort;
    @MockitoBean
    private RenameConversationPort renameConversationPort;
    @MockitoBean
    private DeleteConversationPort deleteConversationPort;
    @MockitoBean
    private TokenGateway tokenGateway;

    @Test
    @WithMockUser(username = "7", roles = "ADMINISTRATOR")
    @DisplayName("list returns summaries with the public id and next cursor, never cached by clients")
    void list() throws Exception {
        when(listConversationsPort.execute(new ListConversationsPort.Input("abc", 5)))
                .thenReturn(new ConversationPage(new ArrayList<>(List.of(CONVERSATION)), "next"));

        mockMvc.perform(get(BASE).param("cursor", "abc").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store, private"))
                .andExpect(jsonPath("$.items[0].conversationId").value("kitchen"))
                .andExpect(jsonPath("$.items[0].title").value("Motores"))
                .andExpect(jsonPath("$.items[0].preview").value("Temos 2."))
                .andExpect(jsonPath("$.items[0].messageCount").value(2))
                .andExpect(jsonPath("$.items[0].lastMessageAt").value("2026-11-07T12:00:00Z"))
                .andExpect(jsonPath("$.nextCursor").value("next"));
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMINISTRATOR")
    @DisplayName("messages carry an ETag and answer 304 when it matches")
    void messagesWithEtag() throws Exception {
        MessagePage page = new MessagePage(new ArrayList<>(List.of(
                new StoredChatMessage(1L, 1, ChatMessage.Role.USER, "Quantos motores?", AnaReply.State.COMPLETED, null, NOW),
                new StoredChatMessage(2L, 2, ChatMessage.Role.ASSISTANT, "Tem", AnaReply.State.PARTIAL, "canceled", NOW))),
                null);
        when(listConversationMessagesPort.execute(new ListConversationMessagesPort.Input("kitchen", null, null)))
                .thenReturn(new ListConversationMessagesPort.Output(CONVERSATION, page, "9-2-latest-50"));

        mockMvc.perform(get(BASE + "/kitchen/messages"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, "\"9-2-latest-50\""))
                .andExpect(jsonPath("$.conversationId").value("kitchen"))
                .andExpect(jsonPath("$.items[0].role").value("user"))
                .andExpect(jsonPath("$.items[1].role").value("assistant"))
                .andExpect(jsonPath("$.items[1].state").value("partial"))
                .andExpect(jsonPath("$.items[1].reason").value("canceled"))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());

        mockMvc.perform(get(BASE + "/kitchen/messages").header(HttpHeaders.IF_NONE_MATCH, "\"9-2-latest-50\""))
                .andExpect(status().isNotModified());
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMINISTRATOR")
    @DisplayName("unknown conversation is 404 and a bad cursor is 400 with the field")
    void errors() throws Exception {
        when(getConversationPort.execute("ghost")).thenThrow(new ConversationNotFoundException("ghost"));
        when(listConversationMessagesPort.execute(any()))
                .thenThrow(new InvalidChatHistoryRequestException("cursor", "cursor is invalid"));

        mockMvc.perform(get(BASE + "/ghost"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONVERSATION_NOT_FOUND"));
        mockMvc.perform(get(BASE + "/kitchen/messages").param("cursor", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("cursor"));
    }

    @Test
    @WithMockUser(username = "7", roles = "ADMINISTRATOR")
    @DisplayName("rename returns the summary and delete returns 204")
    void renameAndDelete() throws Exception {
        when(renameConversationPort.execute(new RenameConversationPort.Input("kitchen", "Estoque")))
                .thenReturn(CONVERSATION);

        mockMvc.perform(patch(BASE + "/kitchen").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Estoque\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value("kitchen"));
        mockMvc.perform(delete(BASE + "/kitchen")).andExpect(status().isNoContent());
        verify(deleteConversationPort).execute("kitchen");
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("customers cannot read chat history")
    void customerForbidden() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isForbidden());
        verify(listConversationsPort, never()).execute(any());
    }

    @Test
    @DisplayName("history needs a token")
    void unauthenticated() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }
}
