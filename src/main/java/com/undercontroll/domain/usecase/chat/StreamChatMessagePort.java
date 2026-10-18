package com.undercontroll.domain.usecase.chat;

import com.undercontroll.domain.gateway.ChatStreamSink;

public interface StreamChatMessagePort {

    AnaChatSession prepare(String message, String conversationId);

    void stream(AnaChatSession session, ChatStreamSink sink);
}
