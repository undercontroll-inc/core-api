package com.undercontroll.application.controller.impl;

import com.undercontroll.application.controller.ChatApi;
import com.undercontroll.application.dto.chat.ChatSuggestionsResponse;
import com.undercontroll.application.dto.chat.SendChatMessageRequest;
import com.undercontroll.application.dto.chat.SendChatMessageResponse;
import com.undercontroll.domain.usecase.chat.GetChatSuggestionsPort;
import com.undercontroll.domain.usecase.chat.SendChatMessagePort;
import com.undercontroll.application.controller.sse.AnaChatSseSupport;
import com.undercontroll.domain.model.chat.AnaReply;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Locale;

@RestController
@RequiredArgsConstructor
public class ChatController implements ChatApi {

    private final SendChatMessagePort sendChatMessagePort;
    private final GetChatSuggestionsPort getChatSuggestionsPort;
    private final AnaChatSseSupport anaChatSseSupport;

    @Override
    public ResponseEntity<SendChatMessageResponse> sendMessage(SendChatMessageRequest message) {
        SendChatMessagePort.Output output = sendChatMessagePort.execute(
                new SendChatMessagePort.Input(message.content(), message.conversationId()));
        AnaReply reply = output.reply();
        return ResponseEntity.ok(new SendChatMessageResponse(
                output.conversationId(),
                reply.content(),
                reply.state().name().toLowerCase(Locale.ROOT),
                reply.reason()
        ));
    }

    @Override
    public ResponseEntity<SseEmitter> streamMessage(SendChatMessageRequest message) {
        return anaChatSseSupport.open(message);
    }

    @Override
    public ResponseEntity<ChatSuggestionsResponse> getSuggestions() {
        return ResponseEntity.ok(new ChatSuggestionsResponse(getChatSuggestionsPort.execute(false)));
    }

    @Override
    public ResponseEntity<ChatSuggestionsResponse> refreshSuggestions() {
        return ResponseEntity.ok(new ChatSuggestionsResponse(getChatSuggestionsPort.execute(true)));
    }
}
