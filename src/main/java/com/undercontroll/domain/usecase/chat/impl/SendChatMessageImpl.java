package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.application.dto.chat.SendChatMessageRequest;
import com.undercontroll.application.dto.chat.SendChatMessageResponse;
import com.undercontroll.domain.gateway.AnaChatGateway;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.usecase.chat.SendChatMessagePort;
import com.undercontroll.infrastructure.logging.LogTiming;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendChatMessageImpl implements SendChatMessagePort {

    private final ObjectProvider<AnaChatGateway> anaChatGateway;
    private final CurrentUserIdPort currentUserIdPort;
    private final ShopSnapshotLoader shopSnapshotLoader;
    private final OrderGateway orderGateway;

    @Override
    public SendChatMessageResponse execute(SendChatMessageRequest request) {
        long started = System.nanoTime();
        AnaConversationPrep.Prepared prepared = AnaConversationPrep.prepare(
                anaChatGateway,
                currentUserIdPort,
                shopSnapshotLoader,
                orderGateway,
                request.content()
        );
        log.info("Ana chat requested userId={} conversationId={}", prepared.userId(), prepared.conversationId());
        String content = prepared.llm().reply(prepared.conversationId(), request.content(), prepared.briefing());
        log.info(
                "Ana chat completed userId={} conversationId={} durationMs={}",
                prepared.userId(),
                prepared.conversationId(),
                LogTiming.millisSince(started)
        );
        return new SendChatMessageResponse(content);
    }
}
