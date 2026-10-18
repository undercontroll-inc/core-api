package com.undercontroll.application.controller.sse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SseChatStreamSinkTest {

    private final ScheduledExecutorService heartbeat = mock(ScheduledExecutorService.class);
    private final ScheduledFuture<?> heartbeatTask = mock(ScheduledFuture.class);
    private final SseEmitter emitter = mock(SseEmitter.class);
    private SseChatStreamSink sink;

    @BeforeEach
    void setUp() {
        doReturn(heartbeatTask).when(heartbeat).scheduleAtFixedRate(any(Runnable.class), anyLong(), anyLong(), any(TimeUnit.class));
        sink = new SseChatStreamSink(heartbeat).bind(emitter);
    }

    private Runnable completionCallback() {
        ArgumentCaptor<Runnable> callback = ArgumentCaptor.forClass(Runnable.class);
        verify(emitter).onCompletion(callback.capture());
        return callback.getValue();
    }

    @Test
    @DisplayName("start schedules the heartbeat and every event is sent to the emitter")
    void sendsEvents() throws IOException {
        sink.start("12");
        sink.status("generating");
        sink.delta("Oi");
        sink.complete("Oi");

        verify(heartbeat).scheduleAtFixedRate(any(Runnable.class), eq(SseChatStreamSink.HEARTBEAT_SECONDS),
                eq(SseChatStreamSink.HEARTBEAT_SECONDS), eq(TimeUnit.SECONDS));
        verify(emitter, times(4)).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).complete();
    }

    @Test
    @DisplayName("error events close the stream and send failures complete the emitter")
    void errorsCloseTheStream() throws IOException {
        doThrow(new IOException("client gone")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        sink.delta("x");
        sink.error("CHAT_UNAVAILABLE", "Ana is currently unavailable.");

        verify(emitter, times(3)).complete();
    }

    @Test
    @DisplayName("heartbeat failures complete the emitter")
    void heartbeatFailure() throws IOException {
        sink.start("12");
        ArgumentCaptor<Runnable> tick = ArgumentCaptor.forClass(Runnable.class);
        verify(heartbeat).scheduleAtFixedRate(tick.capture(), anyLong(), anyLong(), any(TimeUnit.class));
        doThrow(new IOException("broken pipe")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));

        tick.getValue().run();

        verify(emitter).complete();
    }

    @Test
    @DisplayName("closing the emitter cancels the heartbeat and runs the cancel action once")
    void shutdownRunsCancelOnce() {
        AtomicInteger cancelled = new AtomicInteger();
        sink.start("12");
        sink.onCancel(cancelled::incrementAndGet);
        Runnable completion = completionCallback();

        completion.run();
        completion.run();

        verify(heartbeatTask).cancel(true);
        assertThat(cancelled).hasValue(1);
    }

    @Test
    @DisplayName("a cancel action registered after close runs immediately")
    void cancelAfterClose() {
        AtomicInteger cancelled = new AtomicInteger();
        completionCallback().run();

        sink.onCancel(cancelled::incrementAndGet);

        assertThat(cancelled).hasValue(1);
    }
}
