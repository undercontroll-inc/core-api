package com.undercontroll.domain.usecase.chat;

import com.undercontroll.domain.gateway.ChatStreamSink;

public interface StreamChatMessagePort {

    StreamChatSession prepare(String message);

    void stream(StreamChatSession session, ChatStreamSink sink);
}
