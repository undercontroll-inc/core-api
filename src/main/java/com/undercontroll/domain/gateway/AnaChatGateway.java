package com.undercontroll.domain.gateway;

import com.undercontroll.domain.model.chat.AnaReply;
import com.undercontroll.domain.model.chat.AnaTurnRequest;

import java.util.List;

public interface AnaChatGateway {

    AnaReply reply(AnaTurnRequest turn);

    void streamReply(AnaTurnRequest turn, ChatStreamSink sink);

    default List<String> recentConversationTexts(String conversationId) {
        return List.of();
    }
}
