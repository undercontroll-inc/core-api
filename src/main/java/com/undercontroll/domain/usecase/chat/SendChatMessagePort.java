package com.undercontroll.domain.usecase.chat;

import com.undercontroll.domain.model.chat.AnaReply;

public interface SendChatMessagePort {

    Output execute(Input input);

    record Input(String message, String conversationId) {
    }

    record Output(String conversationId, AnaReply reply) {
    }
}
