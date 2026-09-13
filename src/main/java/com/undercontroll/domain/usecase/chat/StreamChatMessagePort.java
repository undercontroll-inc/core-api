package com.undercontroll.domain.usecase.chat;

import com.undercontroll.application.dto.chat.SendChatMessageRequest;
import com.undercontroll.domain.gateway.ChatStreamSink;

public interface StreamChatMessagePort {

    StreamChatSession prepare(SendChatMessageRequest request);

    void stream(StreamChatSession session, ChatStreamSink sink);
}
