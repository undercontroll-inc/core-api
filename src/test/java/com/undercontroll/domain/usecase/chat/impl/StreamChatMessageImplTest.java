package com.undercontroll.domain.usecase.chat.impl;

import com.undercontroll.domain.exception.AnaUnavailableException;
import com.undercontroll.domain.gateway.AnaChatGateway;
import com.undercontroll.domain.gateway.ChatStreamSink;
import com.undercontroll.domain.model.chat.AnaReply;
import com.undercontroll.domain.model.chat.AnaTurnRequest;
import com.undercontroll.domain.support.Deadline;
import com.undercontroll.domain.model.chat.ConversationKey;
import com.undercontroll.domain.usecase.chat.AnaChatSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamChatMessageImplTest {

    private static final AnaChatSession SESSION = new AnaChatSession(
            new ConversationKey(7, "kitchen"),
            new AnaTurnRequest("kitchen", "Oi", "briefing", List.of(), false, Deadline.after(Duration.ofSeconds(60))),
            null,
            true
    );

    @Mock
    private AnaTurnPreparer preparer;

    @Mock
    private AnaChatGateway gateway;

    @InjectMocks
    private StreamChatMessageImpl useCase;

    @Test
    @DisplayName("prepare delegates to the turn preparer")
    void prepares() {
        when(preparer.prepare("Oi", "kitchen")).thenReturn(SESSION);

        assertSame(SESSION, useCase.prepare("Oi", "kitchen"));
    }

    @Test
    @DisplayName("completed streams are recorded before done reaches the client")
    void completedIsRecorded() {
        RecordingSink sink = new RecordingSink();
        ChatStreamSink wrapped = stream(sink);

        wrapped.delta("Tem ");
        wrapped.complete("Tem 2.");
        sink.cancel.run();

        assertEquals(List.of("start:kitchen", "status:generating", "delta:Tem ", "done:Tem 2.", "cancelled"), sink.events);
        verify(preparer).finish(SESSION, AnaReply.completed("Tem 2."));
        verify(preparer, never()).abandon(any());
    }

    @Test
    @DisplayName("partial, failed and cancelled streams only release the conversation once")
    void nonCompletedAreNotRecorded() {
        RecordingSink partialSink = new RecordingSink();
        stream(partialSink).partial("Tem", "max_tokens");
        RecordingSink errorSink = new RecordingSink();
        stream(errorSink).error("CHAT_UNAVAILABLE", "x");
        RecordingSink cancelSink = new RecordingSink();
        stream(cancelSink);
        cancelSink.cancel.run();
        cancelSink.cancel.run();

        assertEquals("partial:max_tokens:Tem", partialSink.events.getLast());
        assertEquals("error:CHAT_UNAVAILABLE", errorSink.events.getLast());
        verify(preparer, times(3)).abandon(SESSION);
        verify(preparer, never()).finish(any(), any());
    }

    @Test
    @DisplayName("emits an error event and releases the conversation when the gateway disappears")
    void gatewayMissing() {
        when(preparer.gateway()).thenThrow(new AnaUnavailableException());
        RecordingSink sink = new RecordingSink();

        useCase.stream(SESSION, sink);

        assertEquals(List.of("error:CHAT_UNAVAILABLE"), sink.events);
        verify(preparer).abandon(SESSION);
    }

    private ChatStreamSink stream(RecordingSink sink) {
        when(preparer.gateway()).thenReturn(gateway);
        ArgumentCaptor<ChatStreamSink> wrapped = ArgumentCaptor.forClass(ChatStreamSink.class);
        useCase.stream(SESSION, sink);
        verify(gateway, atLeastOnce()).streamReply(eq(SESSION.turn()), wrapped.capture());
        ChatStreamSink last = wrapped.getValue();
        last.onCancel(() -> sink.events.add("cancelled"));
        return last;
    }

    private static final class RecordingSink implements ChatStreamSink {

        private final List<String> events = new ArrayList<>();
        private Runnable cancel = () -> {
        };

        @Override
        public void start(String conversationId) {
            events.add("start:" + conversationId);
        }

        @Override
        public void status(String phase) {
            events.add("status:" + phase);
        }

        @Override
        public void delta(String chunk) {
            events.add("delta:" + chunk);
        }

        @Override
        public void complete(String fullContent) {
            events.add("done:" + fullContent);
        }

        @Override
        public void partial(String content, String reason) {
            events.add("partial:" + reason + ":" + content);
        }

        @Override
        public void error(String code, String message) {
            events.add("error:" + code);
        }

        @Override
        public void onCancel(Runnable action) {
            cancel = action;
        }
    }
}
