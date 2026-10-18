package com.undercontroll.infrastructure.ai;

import com.undercontroll.domain.exception.AnaUnavailableException;
import com.undercontroll.domain.gateway.AnaChatGateway;
import com.undercontroll.domain.gateway.ChatStreamSink;
import com.undercontroll.domain.model.chat.AnaReply;
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
    public AnaReply reply(String conversationId, String message, String shopBriefing) {
        long started = System.nanoTime();
        boolean webSearch = isWebSearch(conversationId, message);
        try {
            log.info("Ana chat started conversationId={} tools={}", conversationId, toolsLabel(webSearch));
            ChatResponse response = prompt(conversationId, message, shopBriefing).call().chatResponse();
            String content = text(response);
            if (content.isBlank()) {
                throw new AnaUnavailableException();
            }
            String partialReason = AnaFinishReason.partialReason(AnaFinishReason.of(response));
            log.info(
                    "Ana chat finished conversationId={} tools={} state={} durationMs={}",
                    conversationId,
                    toolsLabel(webSearch),
                    partialReason == null ? "completed" : "partial:" + partialReason,
                    Durations.millisSince(started)
            );
            return partialReason == null ? AnaReply.completed(content) : AnaReply.partial(content, partialReason);
        } catch (AnaUnavailableException ex) {
            log.warn(
                    "Ana chat unavailable conversationId={} durationMs={}",
                    conversationId,
                    Durations.millisSince(started)
            );
            throw ex;
        } catch (RuntimeException ex) {
            log.warn(
                    "Ana chat failed conversationId={} durationMs={} cause={}",
                    conversationId,
                    Durations.millisSince(started),
                    ex.toString()
            );
            throw new AnaUnavailableException(ex);
        }
    }

    @Override
    public void streamReply(String conversationId, String message, String shopBriefing, ChatStreamSink sink) {
        long started = System.nanoTime();
        boolean webSearch = isWebSearch(conversationId, message);
        log.info("Ana chat stream started conversationId={} tools={}", conversationId, toolsLabel(webSearch));
        StreamTurn turn = new StreamTurn(conversationId, started, sink);
        Disposable.Swap subscription = Disposables.swap();
        sink.onCancel(() -> {
            turn.cancelled();
            subscription.dispose();
        });
        try {
            Flux<ChatResponse> responses = prompt(conversationId, message, shopBriefing)
                    .stream()
                    .chatResponse()
                    .limitRate(1)
                    .subscribeOn(Schedulers.boundedElastic());
            subscription.update(responses.subscribe(turn::next, turn::failed, turn::completed));
        } catch (RuntimeException ex) {
            turn.failed(ex);
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

    private ChatClient.ChatClientRequestSpec prompt(String conversationId, String message, String shopBriefing) {
        boolean webSearch = isWebSearch(conversationId, message);
        var spec = anaChatClient.prompt()
                .advisors(advisor -> advisor
                        .param(ChatMemory.CONVERSATION_ID, conversationId)
                        .param(ShopBriefingAdvisor.PARAM, shopBriefing == null ? "" : shopBriefing))
                .user(message);
        if (webSearch) {
            return spec.tools(anaShopTools, anaWebSearchTool);
        }
        return spec.tools(anaShopTools);
    }

    private boolean isWebSearch(String conversationId, String message) {
        return AnaWebSearchNeed.matchesConversation(message, recentConversationTexts(conversationId));
    }

    private static String toolsLabel(boolean webSearch) {
        return webSearch ? "shop+web" : "shop";
    }

    private static String text(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        String text = response.getResult().getOutput().getText();
        return text == null ? "" : text;
    }

    private static final class StreamTurn {

        private final String conversationId;
        private final long started;
        private final ChatStreamSink sink;
        private final StringBuilder content = new StringBuilder();
        private final AtomicBoolean finished = new AtomicBoolean();
        private volatile String finishReason;

        private StreamTurn(String conversationId, long started, ChatStreamSink sink) {
            this.conversationId = conversationId;
            this.started = started;
            this.sink = sink;
        }

        void next(ChatResponse response) {
            String reason = AnaFinishReason.of(response);
            if (reason != null) {
                finishReason = reason;
            }
            String chunk = text(response);
            if (!chunk.isEmpty()) {
                content.append(chunk);
                sink.delta(chunk);
            }
        }

        void completed() {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            String text = content.toString();
            if (text.isBlank()) {
                log.warn("Ana chat stream unavailable conversationId={} durationMs={}",
                        conversationId, Durations.millisSince(started));
                sink.error(AnaUnavailableException.CODE, AnaUnavailableException.MESSAGE);
                return;
            }
            String partialReason = AnaFinishReason.partialReason(finishReason);
            if (partialReason != null) {
                log.warn("Ana chat stream partial conversationId={} reason={} durationMs={}",
                        conversationId, partialReason, Durations.millisSince(started));
                sink.partial(text, partialReason);
                return;
            }
            log.info("Ana chat stream finished conversationId={} finishReason={} durationMs={}",
                    conversationId, finishReason, Durations.millisSince(started));
            sink.complete(text);
        }

        void failed(Throwable error) {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            String text = content.toString();
            log.warn("Ana chat stream failed conversationId={} partial={} durationMs={} cause={}",
                    conversationId, !text.isBlank(), Durations.millisSince(started), error.toString());
            if (text.isBlank()) {
                sink.error(AnaUnavailableException.CODE, AnaUnavailableException.MESSAGE);
            } else {
                sink.partial(text, AnaFinishReason.INTERRUPTED);
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
