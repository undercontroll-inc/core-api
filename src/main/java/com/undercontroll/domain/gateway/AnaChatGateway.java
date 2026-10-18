package com.undercontroll.domain.gateway;

import com.undercontroll.domain.model.chat.AnaReply;

import java.util.List;

public interface AnaChatGateway {

    AnaReply reply(String conversationId, String message, String shopBriefing);

    void streamReply(String conversationId, String message, String shopBriefing, ChatStreamSink sink);

    default List<String> recentConversationTexts(String conversationId) {
        return List.of();
    }
}
