package com.undercontroll.infrastructure.ai;

import com.undercontroll.domain.exception.AnaUnavailableException;
import com.undercontroll.domain.gateway.ChatStreamSink;
import com.undercontroll.domain.model.chat.AnaReply;
import com.undercontroll.domain.model.chat.AnaTurnRequest;
import com.undercontroll.domain.model.chat.ChatMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringAiAnaChatAdapterTest {

    private static final AnaTurnRequest TURN = new AnaTurnRequest("7", "Oi", "briefing", List.of(), false);

    @Mock
    private AnaWebSearchTool webSearchTool;

    @Mock
    private AnaShopTools shopTools;

    @Test
    @DisplayName("history becomes user and assistant messages between the system prompt and the new question")
    void historyGoesIntoPrompt() {
        List<Prompt> prompts = new ArrayList<>();
        ChatModel model = new StubChatModel(List.of("ok"), "STOP", null) {
            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                prompts.add(prompt);
                return super.stream(prompt);
            }
        };
        AnaTurnRequest turn = new AnaTurnRequest("7", "E agora?", "briefing", List.of(
                ChatMessage.user("Me fala do pedido 12"),
                ChatMessage.assistant("Está em análise."),
                ChatMessage.assistant(" ")
        ), false);

        adapter(model).reply(turn);

        List<Message> messages = prompts.getFirst().getInstructions();
        assertEquals(List.of(MessageType.USER, MessageType.ASSISTANT, MessageType.USER),
                messages.stream().map(Message::getMessageType).toList());
        assertEquals("Me fala do pedido 12", messages.get(0).getText());
        assertEquals("E agora?", messages.get(2).getText());
    }

    @Test
    @DisplayName("streams token deltas and completes when the provider reports STOP")
    void streamReply() throws InterruptedException {
        RecordingSink sink = stream(new StubChatModel(List.of("Tem ", "2."), "STOP", null));

        assertEquals(List.of("delta:Tem ", "delta:2.", "done:Tem 2."), sink.events);
    }

    @Test
    @DisplayName("emits an error event when the streamed reply is blank")
    void streamBlank() throws InterruptedException {
        RecordingSink sink = stream(new StubChatModel(List.of(), "STOP", null));

        assertEquals(List.of("error:" + AnaUnavailableException.CODE), sink.events);
    }

    @Test
    @DisplayName("emits an error event when the stream fails before any text")
    void streamFails() throws InterruptedException {
        RecordingSink sink = stream(new StubChatModel(List.of(), null, new IllegalStateException("down")));

        assertEquals(List.of("error:" + AnaUnavailableException.CODE), sink.events);
    }

    @Test
    @DisplayName("a failure after deltas ends as partial, never as done")
    void streamFailsAfterDeltas() throws InterruptedException {
        RecordingSink sink = stream(new StubChatModel(
                List.of("Tem ", "2."),
                null,
                new IllegalStateException("Failed to parse the JSON string.")
        ));

        assertEquals(List.of("delta:Tem ", "delta:2.", "partial:interrupted:Tem 2."), sink.events);
    }

    @Test
    @DisplayName("MAX_TOKENS finish reason ends as partial")
    void streamTruncated() throws InterruptedException {
        RecordingSink sink = stream(new StubChatModel(List.of("Tem "), "MAX_TOKENS", null));

        assertEquals(List.of("delta:Tem ", "partial:max_tokens:Tem "), sink.events);
    }

    @Test
    @DisplayName("a client that is already gone cancels the upstream subscription immediately")
    void cancelBeforeSubscribe() {
        AtomicBoolean upstreamCancelled = new AtomicBoolean();
        CountDownLatch subscribed = new CountDownLatch(1);
        ChatModel model = new StubChatModel(List.of(), null, null) {
            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                return Flux.<ChatResponse>never()
                        .doOnSubscribe(subscription -> subscribed.countDown())
                        .doOnCancel(() -> upstreamCancelled.set(true));
            }
        };
        RecordingSink sink = new RecordingSink() {
            @Override
            public void onCancel(Runnable cancel) {
                cancel.run();
            }
        };

        adapter(model).streamReply(TURN, sink);

        assertTrue(sink.events.isEmpty());
        assertTrue(subscribed.getCount() == 1 || upstreamCancelled.get());
    }

    @Test
    @DisplayName("sync reply reports completed or partial from the finish reason")
    void replyStates() {

        AnaReply completed = adapter(new StubChatModel(List.of("Tem 2."), "STOP", null)).reply(TURN);
        AnaReply partial = adapter(new StubChatModel(List.of("Tem"), "LENGTH", null)).reply(TURN);

        assertTrue(completed.isCompleted());
        assertEquals("Tem 2.", completed.content());
        assertEquals(AnaReply.State.PARTIAL, partial.state());
        assertEquals("max_tokens", partial.reason());
    }

    @Test
    @DisplayName("sync reply turns blank text and provider errors into unavailable")
    void replyFailures() {
        SpringAiAnaChatAdapter blank = adapter(new StubChatModel(List.of(" "), "STOP", null));
        SpringAiAnaChatAdapter failing = adapter(new StubChatModel(List.of(), null, new IllegalStateException("down")));

        assertThrows(AnaUnavailableException.class, () -> blank.reply(TURN));
        assertThrows(AnaUnavailableException.class, () -> failing.reply(TURN));
    }

    @Test
    @DisplayName("sync reply keeps text received before a failure as partial")
    void replyInterrupted() {

        AnaReply reply = adapter(new StubChatModel(List.of("Tem "), null, new IllegalStateException("reset")))
                .reply(TURN);

        assertEquals(AnaReply.State.PARTIAL, reply.state());
        assertEquals("interrupted", reply.reason());
        assertEquals("Tem ", reply.content());
    }

    @Test
    @DisplayName("web search tool is offered only when the turn asks for it")
    void toolsFollowTurnDecision() {
        List<Prompt> prompts = new ArrayList<>();
        ChatModel model = new StubChatModel(List.of("ok"), "STOP", null) {
            @Override
            public Flux<ChatResponse> stream(Prompt prompt) {
                prompts.add(prompt);
                return super.stream(prompt);
            }
        };
        SpringAiAnaChatAdapter adapter = adapter(model);

        adapter.reply(TURN);
        adapter.reply(new AnaTurnRequest("7", "manual?", "briefing", List.of(), true));

        assertEquals(List.of("get_last_announcement", "get_order", "list_orders", "search_components"), toolNames(prompts.get(0)));
        assertTrue(toolNames(prompts.get(1)).contains("search_web"));
    }

    private static List<String> toolNames(Prompt prompt) {
        return ((ToolCallingChatOptions) prompt.getOptions()).getToolCallbacks().stream()
                .map(callback -> callback.getToolDefinition().name())
                .sorted()
                .toList();
    }

    private RecordingSink stream(ChatModel model) throws InterruptedException {
        RecordingSink sink = new RecordingSink();
        adapter(model).streamReply(TURN, sink);
        assertTrue(sink.await());
        return sink;
    }

    private SpringAiAnaChatAdapter adapter(ChatModel chatModel) {
        return new SpringAiAnaChatAdapter(
                ChatClient.builder(chatModel).build(),
                webSearchTool,
                shopTools
        );
    }

    private static class StubChatModel implements ChatModel {

        private final List<String> chunks;
        private final String finishReason;
        private final RuntimeException error;

        private StubChatModel(List<String> chunks, String finishReason, RuntimeException error) {
            this.chunks = chunks;
            this.finishReason = finishReason;
            this.error = error;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            if (error != null) {
                throw error;
            }
            return response(String.join("", chunks), finishReason);
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            List<ChatResponse> responses = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                responses.add(response(chunks.get(i), i == chunks.size() - 1 ? finishReason : null));
            }
            Flux<ChatResponse> body = Flux.fromIterable(responses);
            if (error == null) {
                return body;
            }
            return body.concatWith(Flux.error(error));
        }

        private static ChatResponse response(String text, String finishReason) {
            ChatGenerationMetadata metadata = finishReason == null
                    ? ChatGenerationMetadata.NULL
                    : ChatGenerationMetadata.builder().finishReason(finishReason).build();
            return new ChatResponse(List.of(new Generation(new AssistantMessage(text), metadata)));
        }
    }

    private static class RecordingSink implements ChatStreamSink {

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
        public void partial(String content, String reason) {
            events.add("partial:" + reason + ":" + content);
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
