package com.undercontroll.domain.usecase.chat;

import com.undercontroll.domain.model.chat.AnaReply;

public interface SendChatMessagePort {
    AnaReply execute(String message);
}
