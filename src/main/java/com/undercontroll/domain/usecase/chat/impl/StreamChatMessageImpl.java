package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.application.dto.chat.SendChatMessageRequest;
import com.undercontroll.domain.exception.AnaUnavailableException;
import com.undercontroll.domain.gateway.AnaChatGateway;
import com.undercontroll.domain.gateway.ChatStreamSink;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.usecase.chat.StreamChatMessagePort;
import com.undercontroll.domain.usecase.chat.StreamChatSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamChatMessageImpl implements StreamChatMessagePort {

    static final String GENERATING_PHASE = "generating";

    private final ObjectProvider<AnaChatGateway> anaChatGateway;
    private final CurrentUserIdPort currentUserIdPort;
    private final ShopSnapshotLoader shopSnapshotLoader;
    private final OrderGateway orderGateway;

    @Override
    public StreamChatSession prepare(SendChatMessageRequest request) {
        AnaConversationPrep.Prepared prepared = AnaConversationPrep.prepare(
                anaChatGateway,
                currentUserIdPort,
                shopSnapshotLoader,
                orderGateway,
                request.content()
        );
        log.info(
                "Ana chat stream requested userId={} conversationId={}",
                prepared.userId(),
                prepared.conversationId()
        );
        return prepared.toSession(request.content());
    }

    @Override
    public void stream(StreamChatSession session, ChatStreamSink sink) {
        AnaChatGateway llm = anaChatGateway.getIfAvailable();
        if (llm == null) {
            log.warn("Ana chat gateway is not available");
            sink.error(AnaUnavailableException.CODE, AnaUnavailableException.MESSAGE);
            return;
        }
        sink.start(session.conversationId());
        sink.status(GENERATING_PHASE);
        llm.streamReply(session.conversationId(), session.message(), session.briefing(), sink);
    }
}
