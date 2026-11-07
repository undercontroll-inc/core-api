package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.domain.model.chat.AnaReply;
import com.undercontroll.domain.support.Durations;
import com.undercontroll.domain.usecase.chat.AnaChatSession;
import com.undercontroll.domain.usecase.chat.SendChatMessagePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendChatMessageImpl implements SendChatMessagePort {

    private final AnaTurnPreparer preparer;

    @Override
    public Output execute(Input input) {
        long started = System.nanoTime();
        AnaChatSession session = preparer.prepare(input.message(), input.conversationId());
        log.info("Ana chat requested userId={} conversationId={}", session.key().userId(), session.conversationId());
        AnaReply reply;
        try {
            reply = preparer.gateway().reply(session.turn());
        } catch (RuntimeException ex) {
            preparer.finish(session, null);
            throw ex;
        }
        Long assistantMessageId = preparer.finish(session, reply);
        log.info(
                "Ana chat completed userId={} conversationId={} state={} durationMs={}",
                session.key().userId(),
                session.conversationId(),
                reply.state(),
                Durations.millisSince(started)
        );
        return new Output(session.conversationId(), reply, session.userMessageId(), assistantMessageId);
    }
}
