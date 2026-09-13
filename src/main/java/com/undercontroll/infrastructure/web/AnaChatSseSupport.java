package com.undercontroll.infrastructure.web;

import com.undercontroll.application.dto.chat.SendChatMessageRequest;
import com.undercontroll.domain.usecase.chat.StreamChatMessagePort;
import com.undercontroll.domain.usecase.chat.StreamChatSession;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.Executor;

@Component
public class AnaChatSseSupport {

    private final StreamChatMessagePort streamChatMessagePort;
    private final ObjectProvider<SseChatStreamSink> sinks;
    private final Executor taskExecutor;

    public AnaChatSseSupport(
            StreamChatMessagePort streamChatMessagePort,
            ObjectProvider<SseChatStreamSink> sinks,
            @Qualifier("taskExecutor") Executor taskExecutor
    ) {
        this.streamChatMessagePort = streamChatMessagePort;
        this.sinks = sinks;
        this.taskExecutor = taskExecutor;
    }

    public ResponseEntity<SseEmitter> open(SendChatMessageRequest request) {
        StreamChatSession session = streamChatMessagePort.prepare(request);
        SseEmitter emitter = new SseEmitter(SseChatStreamSink.TIMEOUT_MS);
        SseChatStreamSink sink = sinks.getObject().bind(emitter);
        taskExecutor.execute(new DelegatingSecurityContextRunnable(
                () -> streamChatMessagePort.stream(session, sink)));
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform")
                .header("X-Accel-Buffering", "no")
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }
}
