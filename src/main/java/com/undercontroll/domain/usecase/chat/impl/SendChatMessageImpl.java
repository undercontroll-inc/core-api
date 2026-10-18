package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.domain.gateway.AnaChatGateway;
import com.undercontroll.domain.gateway.CurrentUserIdPort;
import com.undercontroll.domain.gateway.ShopSnapshotLoader;
import com.undercontroll.domain.gateway.OrderGateway;
import com.undercontroll.domain.usecase.chat.SendChatMessagePort;
import com.undercontroll.domain.support.Durations;
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
    public String execute(String message) {
        long started = System.nanoTime();
        AnaConversationPrep.Prepared prepared = AnaConversationPrep.prepare(
                anaChatGateway,
                currentUserIdPort,
                shopSnapshotLoader,
                orderGateway,
                message
        );
        log.info("Ana chat requested userId={} conversationId={}", prepared.userId(), prepared.conversationId());
        String content = prepared.llm().reply(prepared.conversationId(), message, prepared.briefing());
        log.info(
                "Ana chat completed userId={} conversationId={} durationMs={}",
                prepared.userId(),
                prepared.conversationId(),
                Durations.millisSince(started)
        );
        return content;
    }
}
