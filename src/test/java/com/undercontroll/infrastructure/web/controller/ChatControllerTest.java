package com.undercontroll.infrastructure.web.controller;

import com.undercontroll.application.controller.impl.ChatController;
import com.undercontroll.application.dto.chat.ChatSuggestionsResponse;
import com.undercontroll.application.dto.chat.SendChatMessageResponse;
import com.undercontroll.domain.exception.AnaUnavailableException;
import com.undercontroll.domain.gateway.ChatStreamSink;
import com.undercontroll.domain.usecase.chat.GetChatSuggestionsPort;
import com.undercontroll.domain.usecase.chat.SendChatMessagePort;
import com.undercontroll.domain.usecase.chat.StreamChatMessagePort;
import com.undercontroll.domain.usecase.chat.StreamChatSession;
import com.undercontroll.infrastructure.config.AsyncConfig;
import com.undercontroll.infrastructure.config.RateLimitProperties;
import com.undercontroll.infrastructure.config.SecurityConfig;
import com.undercontroll.infrastructure.handler.ChatExceptionHandler;
import com.undercontroll.infrastructure.logging.MdcTaskDecorator;
import com.undercontroll.infrastructure.service.TokenServce;
import com.undercontroll.infrastructure.web.AnaChatSseSupport;
import com.undercontroll.infrastructure.web.SseChatStreamSink;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({
        SecurityConfig.class,
        RateLimitProperties.class,
        ChatExceptionHandler.class,
        AnaChatSseSupport.class,
        SseChatStreamSink.class,
        AsyncConfig.class,
        MdcTaskDecorator.class
})
@AutoConfigureMockMvc
@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SendChatMessagePort sendChatMessagePort;

    @MockitoBean
    private StreamChatMessagePort streamChatMessagePort;

    @MockitoBean
    private GetChatSuggestionsPort getChatSuggestionsPort;

    @MockitoBean
    private TokenServce tokenServce;

    @Test
    @WithMockUser(username = "12", roles = "ADMINISTRATOR")
    @DisplayName("POST /v1/api/chats/messages returns the assistant reply")
    void sendMessage() throws Exception {
        when(sendChatMessagePort.execute(any())).thenReturn(new SendChatMessageResponse("Tem 2 consertos abertos."));

        mockMvc.perform(post("/v1/api/chats/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Quais consertos estão abertos?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Tem 2 consertos abertos."));
    }

    @Test
    @WithMockUser(username = "12", roles = "ADMINISTRATOR")
    @DisplayName("POST /v1/api/chats/messages returns 503 when Ana is unavailable")
    void unavailable() throws Exception {
        when(sendChatMessagePort.execute(any())).thenThrow(new AnaUnavailableException());

        mockMvc.perform(post("/v1/api/chats/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Oi\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("CHAT_UNAVAILABLE"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    @DisplayName("chat endpoints are forbidden for customers")
    void forbiddenForCustomer() throws Exception {
        mockMvc.perform(post("/v1/api/chats/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Oi\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/v1/api/chats/suggestions"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/api/chats/messages/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Oi\"}"))
                .andExpect(status().isForbidden());
        verify(sendChatMessagePort, never()).execute(any());
        verify(streamChatMessagePort, never()).prepare(any());
    }

    @Test
    @WithMockUser(username = "12", roles = "ADMINISTRATOR")
    @DisplayName("POST /v1/api/chats/messages/stream returns 503 JSON before opening SSE")
    void streamUnavailable() throws Exception {
        when(streamChatMessagePort.prepare(any())).thenThrow(new AnaUnavailableException());

        mockMvc.perform(post("/v1/api/chats/messages/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Oi\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("CHAT_UNAVAILABLE"));
        verify(streamChatMessagePort, never()).stream(any(), any());
    }

    @Test
    @WithMockUser(username = "12", roles = "ADMINISTRATOR")
    @DisplayName("POST /v1/api/chats/messages/stream emits start, delta and done events")
    void streamMessage() throws Exception {
        when(streamChatMessagePort.prepare(any())).thenReturn(new StreamChatSession("12", "Oi", "briefing"));
        doAnswer(invocation -> {
            ChatStreamSink sink = invocation.getArgument(1);
            sink.start("12");
            sink.status("generating");
            sink.delta("Tem ");
            sink.complete("Tem 2 consertos abertos.");
            return null;
        }).when(streamChatMessagePort).stream(any(), any());

        MvcResult result = mockMvc.perform(post("/v1/api/chats/messages/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Oi\"}"))
                .andReturn();
        if (result.getRequest().isAsyncStarted()) {
            result.getAsyncResult();
            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform"))
                    .andExpect(header().string("X-Accel-Buffering", "no"))
                    .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                    .andExpect(content().string(containsString("event:start")))
                    .andExpect(content().string(containsString("\"conversationId\":\"12\"")))
                    .andExpect(content().string(containsString("event:delta")))
                    .andExpect(content().string(containsString("event:done")))
                    .andExpect(content().string(containsString("Tem 2 consertos abertos.")));
            return;
        }

        assertEquals(200, result.getResponse().getStatus());
        assertTrue(result.getResponse().getContentAsString().contains("event:start"));
        assertTrue(result.getResponse().getContentAsString().contains("event:done"));
    }

    @Test
    @WithMockUser(username = "12", roles = "ADMINISTRATOR")
    @DisplayName("GET /v1/api/chats/suggestions returns stored questions")
    void getSuggestions() throws Exception {
        when(getChatSuggestionsPort.execute(anyBoolean()))
                .thenReturn(new ChatSuggestionsResponse(List.of("Pergunta 1", "Pergunta 2")));

        mockMvc.perform(get("/v1/api/chats/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions[0]").value("Pergunta 1"));
    }

    @Test
    @WithMockUser(username = "12", roles = "ADMINISTRATOR")
    @DisplayName("POST /v1/api/chats/suggestions refreshes questions")
    void refreshSuggestions() throws Exception {
        when(getChatSuggestionsPort.execute(anyBoolean()))
                .thenReturn(new ChatSuggestionsResponse(List.of("Nova")));

        mockMvc.perform(post("/v1/api/chats/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions[0]").value("Nova"));
    }
}
