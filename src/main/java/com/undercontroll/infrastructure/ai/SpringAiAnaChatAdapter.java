package com.undercontroll.infrastructure.ai;

import com.undercontroll.domain.exception.AnaUnavailableException;
import com.undercontroll.domain.gateway.AnaChatGateway;
import com.undercontroll.domain.gateway.ChatStreamSink;
import com.undercontroll.domain.model.chat.AnaReply;
import com.undercontroll.domain.model.chat.AnaTurnRequest;
import com.undercontroll.domain.support.Durations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class SpringAiAnaChatAdapter implements AnaChatGateway {

    private final ChatClient anaChatClient;
    private final ChatMemory anaChatMemory;
    private final AnaWebSearchTool anaWebSearchTool;
    private final AnaShopTools anaShopTools;

    public SpringAiAnaChatAdapter(
            ChatClient anaChatClient,
            ChatMemory anaChatMemory,
            AnaWebSearchTool anaWebSearchTool,
            AnaShopTools anaShopTools
    ) {
        this.anaChatClient = anaChatClient;
        this.anaChatMemory = anaChatMemory;
        this.anaWebSearchTool = anaWebSearchTool;
        this.anaShopTools = anaShopTools;
    }

    @Override
    public AnaReply reply(AnaTurnRequest turn) {
        long started = System.nanoTime();
        log.info("Ana chat started conversationId={} tools={}", turn.conversationId(), toolsLabel(turn));
        TurnText text = new TurnText();
        try {
            responses(turn).doOnNext(text::add).blockLast();
        } catch (RuntimeException ex) {
            if (text.isBlank()) {
                log.warn("Ana chat failed conversationId={} durationMs={} cause={}",
                        turn.conversationId(), Durations.millisSince(started), ex.toString());
                throw new AnaUnavailableException(ex);
            }
            log.warn("Ana chat interrupted conversationId={} durationMs={} cause={}",
                    turn.conversationId(), Durations.millisSince(started), ex.toString());
            return AnaReply.partial(text.content(), AnaFinishReason.INTERRUPTED);
        }
        if (text.isBlank()) {
            log.warn("Ana chat unavailable conversationId={} durationMs={}",
                    turn.conversationId(), Durations.millisSince(started));
            throw new AnaUnavailableException();
        }
        AnaReply reply = text.reply();
        log.info("Ana chat finished conversationId={} state={} reason={} durationMs={}",
                turn.conversationId(), reply.state(), reply.reason(), Durations.millisSince(started));
        return reply;
    }

    @Override
    public void streamReply(AnaTurnRequest turn, ChatStreamSink sink) {
        long started = System.nanoTime();
        log.info("Ana chat stream started conversationId={} tools={}", turn.conversationId(), toolsLabel(turn));
        StreamTurn stream = new StreamTurn(turn.conversationId(), started, sink);
        Disposable.Swap subscription = Disposables.swap();
        sink.onCancel(() -> {
            stream.cancelled();
            subscription.dispose();
        });
        try {
            subscription.update(responses(turn)
                    .limitRate(1)
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(stream::next, stream::failed, stream::completed));
        } catch (RuntimeException ex) {
            stream.failed(ex);
        }
    }

    @Override
    public List<String> recentConversationTexts(String conversationId) {
        if (conversationId == null || conversationId.isBlank() || anaChatMemory == null) {
            return List.of();
        }
        try {
            return anaChatMemory.get(conversationId).stream()
                    .map(Message::getText)
                    .filter(text -> text != null && !text.isBlank())
                    .toList();
        } catch (RuntimeException ex) {
            log.warn("Ana chat memory read failed conversationId={} cause={}", conversationId, ex.toString());
            return List.of();
        }
    }

    private Flux<ChatResponse> responses(AnaTurnRequest turn) {
        var spec = anaChatClient.prompt()
                .advisors(advisor -> advisor
                        .param(ChatMemory.CONVERSATION_ID, turn.conversationId())
                        .param(ShopBriefingAdvisor.PARAM, turn.briefing() == null ? "" : turn.briefing()))
                .user(turn.message());
        if (turn.webSearch()) {
            spec = spec.tools(anaShopTools, anaWebSearchTool);
        } else {
            spec = spec.tools(anaShopTools);
        }
        return spec.stream().chatResponse();
    }

    private static String toolsLabel(AnaTurnRequest turn) {
        return turn.webSearch() ? "shop+web" : "shop";
    }

    private static String text(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        String text = response.getResult().getOutput().getText();
        return text == null ? "" : text;
    }

    private static final class TurnText {

        private final StringBuilder content = new StringBuilder();
        private volatile String finishReason;

        String add(ChatResponse response) {
            String reason = AnaFinishReason.of(response);
            if (reason != null) {
                finishReason = reason;
            }
            String chunk = text(response);
            content.append(chunk);
            return chunk;
        }

        boolean isBlank() {
            return content.toString().isBlank();
        }

        String content() {
            return content.toString();
        }

        AnaReply reply() {
            String partialReason = AnaFinishReason.partialReason(finishReason);
            return partialReason == null
                    ? AnaReply.completed(content())
                    : AnaReply.partial(content(), partialReason);
        }
    }

    private static final class StreamTurn {

        private final String conversationId;
        private final long started;
        private final ChatStreamSink sink;
        private final TurnText text = new TurnText();
        private final AtomicBoolean finished = new AtomicBoolean();

        private StreamTurn(String conversationId, long started, ChatStreamSink sink) {
            this.conversationId = conversationId;
            this.started = started;
            this.sink = sink;
        }

        void next(ChatResponse response) {
            String chunk = text.add(response);
            if (!chunk.isEmpty()) {
                sink.delta(chunk);
            }
        }

        void completed() {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            if (text.isBlank()) {
                log.warn("Ana chat stream unavailable conversationId={} durationMs={}",
                        conversationId, Durations.millisSince(started));
                sink.error(AnaUnavailableException.CODE, AnaUnavailableException.MESSAGE);
                return;
            }
            AnaReply reply = text.reply();
            log.info("Ana chat stream finished conversationId={} state={} reason={} durationMs={}",
                    conversationId, reply.state(), reply.reason(), Durations.millisSince(started));
            if (reply.isCompleted()) {
                sink.complete(reply.content());
            } else {
                sink.partial(reply.content(), reply.reason());
            }
        }

        void failed(Throwable error) {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            log.warn("Ana chat stream failed conversationId={} partial={} durationMs={} cause={}",
                    conversationId, !text.isBlank(), Durations.millisSince(started), error.toString());
            if (text.isBlank()) {
                sink.error(AnaUnavailableException.CODE, AnaUnavailableException.MESSAGE);
            } else {
                sink.partial(text.content(), AnaFinishReason.INTERRUPTED);
            }
        }

        void cancelled() {
            if (finished.compareAndSet(false, true)) {
                log.info("Ana chat stream cancelled conversationId={} durationMs={}",
                        conversationId, Durations.millisSince(started));
            }
        }
    }
}
