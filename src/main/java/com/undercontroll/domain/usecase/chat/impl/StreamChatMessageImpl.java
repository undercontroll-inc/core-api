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

import java.util.Locale;
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
        sink.start(session.conversationId(), session.userMessageId());
        sink.status(GENERATING_PHASE);
        llm.streamReply(session.turn(), new RecordingSink(session, sink));
    }

    private final class RecordingSink implements ChatStreamSink {

        static final String CANCELED = "canceled";

        private final AnaChatSession session;
        private final ChatStreamSink delegate;
        private final AtomicBoolean settled = new AtomicBoolean();
        private final StringBuilder streamed = new StringBuilder();

        private RecordingSink(AnaChatSession session, ChatStreamSink delegate) {
            this.session = session;
            this.delegate = delegate;
        }

        @Override
        public void start(String conversationId) {
            delegate.start(conversationId, session.userMessageId());
        }

        @Override
        public void status(String phase) {
            delegate.status(phase);
        }

        @Override
        public void delta(String chunk) {
            synchronized (streamed) {
                streamed.append(chunk);
            }
            delegate.delta(chunk);
        }

        @Override
        public void complete(String fullContent) {
            Long messageId = settle(AnaReply.completed(fullContent));
            delegate.complete(fullContent, messageId);
        }

        @Override
        public void partial(String content, String reason) {
            Long messageId = settle(AnaReply.partial(content, reason));
            delegate.partial(content, reason, messageId);
        }

        @Override
        public void error(String code, String message) {
            settleStreamed(code == null ? null : code.toLowerCase(Locale.ROOT));
            delegate.error(code, message);
        }

        @Override
        public void onCancel(Runnable cancel) {
            delegate.onCancel(() -> {
                settleStreamed(CANCELED);
                cancel.run();
            });
        }

        private Long settle(AnaReply reply) {
            if (settled.compareAndSet(false, true)) {
                return preparer.finish(session, reply);
            }
            return null;
        }

        private void settleStreamed(String reason) {
            String content;
            synchronized (streamed) {
                content = streamed.toString();
            }
            if (content.isBlank()) {
                if (settled.compareAndSet(false, true)) {
                    preparer.abandon(session);
                }
                return;
            }
            settle(AnaReply.partial(content, reason));
        }
    }
}
