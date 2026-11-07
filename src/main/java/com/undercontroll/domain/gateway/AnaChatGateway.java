package com.undercontroll.domain.gateway;

import com.undercontroll.domain.model.chat.AnaReply;
import com.undercontroll.domain.model.chat.AnaTurnRequest;

public interface AnaChatGateway {

    AnaReply reply(AnaTurnRequest turn);

    void streamReply(AnaTurnRequest turn, ChatStreamSink sink);
}
