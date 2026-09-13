package com.undercontroll.infrastructure.web;

import com.undercontroll.application.dto.chat.ChatStreamDeltaEvent;
import com.undercontroll.application.dto.chat.ChatStreamDoneEvent;
import com.undercontroll.application.dto.chat.ChatStreamErrorEvent;
import com.undercontroll.application.dto.chat.ChatStreamStartEvent;
import com.undercontroll.application.dto.chat.ChatStreamStatusEvent;
import com.undercontroll.domain.gateway.ChatStreamSink;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SseChatStreamSink implements ChatStreamSink {

    public static final long TIMEOUT_MS = 120_000L;
    static final long HEARTBEAT_SECONDS = 5L;

    private final ScheduledExecutorService heartbeat;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicReference<Runnable> cancel = new AtomicReference<>();
    private final AtomicReference<ScheduledFuture<?>> heartbeatTask = new AtomicReference<>();
    private SseEmitter emitter;

    public SseChatStreamSink(@Qualifier("anaSseHeartbeat") ScheduledExecutorService heartbeat) {
        this.heartbeat = heartbeat;
    }

    public SseChatStreamSink bind(SseEmitter emitter) {
        this.emitter = emitter;
        emitter.onCompletion(this::shutdown);
        emitter.onTimeout(this::shutdown);
        emitter.onError(error -> shutdown());
        return this;
    }

    @Override
    public void start(String conversationId) {
        startHeartbeat();
        send("start", new ChatStreamStartEvent(conversationId));
    }

    @Override
    public void status(String phase) {
        send("status", new ChatStreamStatusEvent(phase));
    }

    @Override
    public void delta(String chunk) {
        send("delta", new ChatStreamDeltaEvent(chunk));
    }

    @Override
    public void complete(String fullContent) {
        send("done", new ChatStreamDoneEvent(fullContent));
        emitter.complete();
    }

    @Override
    public void error(String code, String message) {
        send("error", new ChatStreamErrorEvent(code, message));
        emitter.complete();
    }

    @Override
    public void onCancel(Runnable action) {
        cancel.set(action);
        if (closed.get() && action != null) {
            action.run();
        }
    }

    private void startHeartbeat() {
        heartbeatTask.set(heartbeat.scheduleAtFixedRate(
                this::heartbeat,
                HEARTBEAT_SECONDS,
                HEARTBEAT_SECONDS,
                TimeUnit.SECONDS
        ));
    }

    private void heartbeat() {
        try {
            emitter.send(SseEmitter.event().comment("keepalive"));
        } catch (IOException ex) {
            emitter.complete();
        }
    }

    private void send(String name, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(name).data(payload, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException ex) {
            emitter.complete();
        }
    }

    private void shutdown() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        ScheduledFuture<?> task = heartbeatTask.get();
        if (task != null) {
            task.cancel(true);
        }
        Runnable action = cancel.get();
        if (action != null) {
            action.run();
        }
    }
}
