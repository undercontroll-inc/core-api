package com.undercontroll.application.controller.impl;

import com.undercontroll.application.controller.ChatApi;
import com.undercontroll.application.dto.chat.ChatSuggestionsResponse;
import com.undercontroll.application.dto.chat.SendChatMessageRequest;
import com.undercontroll.application.dto.chat.SendChatMessageResponse;
import com.undercontroll.domain.usecase.chat.GetChatSuggestionsPort;
import com.undercontroll.domain.usecase.chat.SendChatMessagePort;
import com.undercontroll.infrastructure.web.AnaChatSseSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class ChatController implements ChatApi {

    private final SendChatMessagePort sendChatMessagePort;
    private final GetChatSuggestionsPort getChatSuggestionsPort;
    private final AnaChatSseSupport anaChatSseSupport;

    @Override
    public ResponseEntity<SendChatMessageResponse> sendMessage(SendChatMessageRequest message) {
        return ResponseEntity.ok(sendChatMessagePort.execute(message));
    }

    @Override
    public ResponseEntity<SseEmitter> streamMessage(SendChatMessageRequest message) {
        return anaChatSseSupport.open(message);
    }

    @Override
    public ResponseEntity<ChatSuggestionsResponse> getSuggestions() {
        return ResponseEntity.ok(getChatSuggestionsPort.execute(false));
    }

    @Override
    public ResponseEntity<ChatSuggestionsResponse> refreshSuggestions() {
        return ResponseEntity.ok(getChatSuggestionsPort.execute(true));
    }
}
