package com.undercontroll.infrastructure.ai;

import com.undercontroll.domain.exception.AnaUnavailableException;
import com.undercontroll.domain.gateway.AnaChatGateway;
import com.undercontroll.domain.gateway.ChatStreamSink;
import com.undercontroll.infrastructure.logging.LogTiming;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
    public String reply(String conversationId, String message, String shopBriefing) {
        long started = System.nanoTime();
        boolean webSearch = isWebSearch(conversationId, message);
        try {
            log.info("Ana chat started conversationId={} tools={}", conversationId, toolsLabel(webSearch));
            String content = prompt(conversationId, message, shopBriefing).call().content();
            if (content == null || content.isBlank()) {
                throw new AnaUnavailableException();
            }
            log.info(
                    "Ana chat finished conversationId={} tools={} durationMs={}",
                    conversationId,
                    toolsLabel(webSearch),
                    LogTiming.millisSince(started)
            );
            return content;
        } catch (AnaUnavailableException ex) {
            log.warn(
                    "Ana chat unavailable conversationId={} durationMs={}",
                    conversationId,
                    LogTiming.millisSince(started)
            );
            throw ex;
        } catch (RuntimeException ex) {
            log.warn(
                    "Ana chat failed conversationId={} durationMs={} cause={}",
                    conversationId,
                    LogTiming.millisSince(started),
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
        StringBuilder full = new StringBuilder();
        AtomicBoolean finished = new AtomicBoolean();
        try {
            Flux<String> tokens = prompt(conversationId, message, shopBriefing)
                    .stream()
                    .content()
                    .doOnNext(chunk -> {
                        if (chunk != null && !chunk.isEmpty()) {
                            full.append(chunk);
                        }
                    })
                    .onErrorResume(error -> {
                        if (full.isEmpty()) {
                            return Flux.error(error);
                        }
                        log.warn(
                                "Ana chat stream recovered conversationId={} durationMs={} cause={}",
                                conversationId,
                                LogTiming.millisSince(started),
                                error.toString()
                        );
                        return Flux.empty();
                    })
                    .limitRate(1)
                    .subscribeOn(Schedulers.boundedElastic());
            AtomicReference<Disposable> subscription = new AtomicReference<>();
            sink.onCancel(() -> {
                Disposable disposable = subscription.get();
                if (disposable != null) {
                    disposable.dispose();
                }
            });
            subscription.set(tokens.subscribe(
                    chunk -> {
                        if (chunk == null || chunk.isEmpty()) {
                            return;
                        }
                        sink.delta(chunk);
                    },
                    error -> {
                        if (!finished.compareAndSet(false, true)) {
                            return;
                        }
                        log.warn(
                                "Ana chat stream failed conversationId={} durationMs={} cause={}",
                                conversationId,
                                LogTiming.millisSince(started),
                                error.toString()
                        );
                        sink.error(AnaUnavailableException.CODE, AnaUnavailableException.MESSAGE);
                    },
                    () -> {
                        if (!finished.compareAndSet(false, true)) {
                            return;
                        }
                        String content = full.toString();
                        if (content.isBlank()) {
                            log.warn(
                                    "Ana chat stream unavailable conversationId={} durationMs={}",
                                    conversationId,
                                    LogTiming.millisSince(started)
                            );
                            sink.error(AnaUnavailableException.CODE, AnaUnavailableException.MESSAGE);
                            return;
                        }
                        log.info(
                                "Ana chat stream finished conversationId={} tools={} durationMs={}",
                                conversationId,
                                toolsLabel(webSearch),
                                LogTiming.millisSince(started)
                        );
                        sink.complete(content);
                    }
            ));
        } catch (RuntimeException ex) {
            log.warn(
                    "Ana chat stream failed conversationId={} durationMs={} cause={}",
                    conversationId,
                    LogTiming.millisSince(started),
                    ex.toString()
            );
            sink.error(AnaUnavailableException.CODE, AnaUnavailableException.MESSAGE);
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
}
