package com.undercontroll.infrastructure.ai;

import com.undercontroll.domain.exception.AnaUnavailableException;
import com.undercontroll.domain.gateway.ChatStreamSink;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringAiAnaChatAdapterTest {

    @Mock
    private ChatMemory chatMemory;

    @Mock
    private AnaWebSearchTool webSearchTool;

    @Mock
    private AnaShopTools shopTools;

    @Test
    @DisplayName("reads conversation texts so follow-ups can reuse cited orders")
    void recentConversationTexts() {
        when(chatMemory.get("7")).thenReturn(List.of(
                new UserMessage("Me fala do pedido 12"),
                new AssistantMessage("O liquidificador da Maria ainda não foi olhado.")
        ));
        SpringAiAnaChatAdapter adapter = new SpringAiAnaChatAdapter(
                ChatClient.builder(new StubChatModel(List.of("ok"), null)).build(),
                chatMemory,
                webSearchTool,
                shopTools
        );

        assertEquals(
                List.of("Me fala do pedido 12", "O liquidificador da Maria ainda não foi olhado."),
                adapter.recentConversationTexts("7"));
        assertEquals(List.of(), adapter.recentConversationTexts(" "));
    }

    @Test
    @DisplayName("streams token deltas and completes with the full reply")
    void streamReply() throws InterruptedException {
        when(chatMemory.get("7")).thenReturn(List.of());
        SpringAiAnaChatAdapter adapter = adapter(new StubChatModel(List.of("Tem ", "2."), null));
        RecordingSink sink = new RecordingSink();

        adapter.streamReply("7", "Oi", "briefing", sink);

        assertTrue(sink.await());
        assertEquals(List.of("delta:Tem ", "delta:2.", "done:Tem 2."), sink.events);
    }

    @Test
    @DisplayName("emits an error event when the streamed reply is blank")
    void streamBlank() throws InterruptedException {
        when(chatMemory.get("7")).thenReturn(List.of());
        SpringAiAnaChatAdapter adapter = adapter(new StubChatModel(List.of(), null));
        RecordingSink sink = new RecordingSink();

        adapter.streamReply("7", "Oi", "briefing", sink);

        assertTrue(sink.await());
        assertEquals(List.of("error:" + AnaUnavailableException.CODE), sink.events);
    }

    @Test
    @DisplayName("emits an error event when the stream fails")
    void streamFails() throws InterruptedException {
        when(chatMemory.get("7")).thenReturn(List.of());
        SpringAiAnaChatAdapter adapter = adapter(new StubChatModel(List.of(), new IllegalStateException("down")));
        RecordingSink sink = new RecordingSink();

        adapter.streamReply("7", "Oi", "briefing", sink);

        assertTrue(sink.await());
        assertEquals(List.of("error:" + AnaUnavailableException.CODE), sink.events);
    }

    @Test
    @DisplayName("completes with the streamed text when Gemini fails after emitting deltas")
    void streamRecoversAfterDeltas() throws InterruptedException {
        when(chatMemory.get("7")).thenReturn(List.of());
        SpringAiAnaChatAdapter adapter = adapter(new StubChatModel(
                List.of("Tem ", "2."),
                new IllegalStateException("Failed to parse the JSON string.")
        ));
        RecordingSink sink = new RecordingSink();

        adapter.streamReply("7", "Oi", "briefing", sink);

        assertTrue(sink.await());
        assertEquals(List.of("delta:Tem ", "delta:2.", "done:Tem 2."), sink.events);
    }

    private SpringAiAnaChatAdapter adapter(ChatModel chatModel) {
        return new SpringAiAnaChatAdapter(
                ChatClient.builder(chatModel).build(),
                chatMemory,
                webSearchTool,
                shopTools
        );
    }

    private static final class StubChatModel implements ChatModel {

        private final List<String> chunks;
        private final RuntimeException error;

        private StubChatModel(List<String> chunks, RuntimeException error) {
            this.chunks = chunks;
            this.error = error;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage(String.join("", chunks)))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            Flux<ChatResponse> body = Flux.fromIterable(chunks)
                    .map(chunk -> new ChatResponse(List.of(new Generation(new AssistantMessage(chunk)))));
            if (error == null) {
                return body;
            }
            return body.concatWith(Flux.error(error));
        }
    }

    private static final class RecordingSink implements ChatStreamSink {

        private final List<String> events = new ArrayList<>();
        private final CountDownLatch done = new CountDownLatch(1);

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
            done.countDown();
        }

        @Override
        public void error(String code, String message) {
            events.add("error:" + code);
            done.countDown();
        }

        boolean await() throws InterruptedException {
            return done.await(3, TimeUnit.SECONDS);
        }
    }
}
