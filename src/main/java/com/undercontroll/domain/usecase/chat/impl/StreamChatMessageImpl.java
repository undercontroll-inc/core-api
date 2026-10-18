package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.domain.exception.AnaUnavailableException;
import com.undercontroll.domain.gateway.AnaChatGateway;
import com.undercontroll.domain.gateway.ChatStreamSink;
import com.undercontroll.domain.model.chat.AnaReply;
import com.undercontroll.domain.usecase.chat.AnaChatSession;
import com.undercontroll.domain.usecase.chat.StreamChatMessagePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamChatMessageImpl implements StreamChatMessagePort {

    static final String GENERATING_PHASE = "generating";

    private final AnaTurnPreparer preparer;

    @Override
    public AnaChatSession prepare(String message, String conversationId) {
        AnaChatSession session = preparer.prepare(message, conversationId);
        log.info(
                "Ana chat stream requested userId={} conversationId={}",
                session.key().userId(),
                session.conversationId()
        );
        return session;
    }

    @Override
    public void stream(AnaChatSession session, ChatStreamSink sink) {
        AnaChatGateway llm;
        try {
            llm = preparer.gateway();
        } catch (AnaUnavailableException ex) {
            log.warn("Ana chat gateway is not available");
            preparer.abandon(session);
            sink.error(AnaUnavailableException.CODE, AnaUnavailableException.MESSAGE);
            return;
        }
        sink.start(session.conversationId());
        sink.status(GENERATING_PHASE);
        llm.streamReply(session.turn(), new RecordingSink(session, sink));
    }

    private final class RecordingSink implements ChatStreamSink {

        private final AnaChatSession session;
        private final ChatStreamSink delegate;
        private final AtomicBoolean settled = new AtomicBoolean();

        private RecordingSink(AnaChatSession session, ChatStreamSink delegate) {
            this.session = session;
            this.delegate = delegate;
        }

        @Override
        public void start(String conversationId) {
            delegate.start(conversationId);
        }

        @Override
        public void status(String phase) {
            delegate.status(phase);
        }

        @Override
        public void delta(String chunk) {
            delegate.delta(chunk);
        }

        @Override
        public void complete(String fullContent) {
            if (settled.compareAndSet(false, true)) {
                preparer.finish(session, AnaReply.completed(fullContent));
            }
            delegate.complete(fullContent);
        }

        @Override
        public void partial(String content, String reason) {
            settle();
            delegate.partial(content, reason);
        }

        @Override
        public void error(String code, String message) {
            settle();
            delegate.error(code, message);
        }

        @Override
        public void onCancel(Runnable cancel) {
            delegate.onCancel(() -> {
                settle();
                cancel.run();
            });
        }

        private void settle() {
            if (settled.compareAndSet(false, true)) {
                preparer.abandon(session);
            }
        }
    }
}
